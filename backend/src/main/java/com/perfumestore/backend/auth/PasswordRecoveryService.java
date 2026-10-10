package com.perfumestore.backend.auth;

import java.time.Instant;
import java.util.UUID;

import com.perfumestore.backend.config.PasswordRecoveryProperties;
import com.perfumestore.backend.config.RateLimitProperties;
import com.perfumestore.backend.notification.EmailSender;
import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.user.AppUser;
import com.perfumestore.backend.user.EmailNormalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordRecoveryService.class);
    private final UserService users;
    private final PasswordResetTokenRepository tokens;
    private final SessionInvalidationRepository sessions;
    private final EmailSender emailSender;
    private final PasswordRecoveryProperties properties;
    private final AttemptRateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    public PasswordRecoveryService(UserService users, PasswordResetTokenRepository tokens,
            SessionInvalidationRepository sessions, EmailSender emailSender, PasswordRecoveryProperties properties,
            AttemptRateLimiter rateLimiter, RateLimitProperties rateLimitProperties) {
        this.users = users;
        this.tokens = tokens;
        this.sessions = sessions;
        this.emailSender = emailSender;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Transactional
    public void request(String email, String remoteAddress) {
        String normalized = EmailNormalizer.normalize(email);
        String key = "recovery:" + normalized + ":" + remoteAddress;
        rateLimiter.check(key, rateLimitProperties.getRecoveryMaxAttempts(), rateLimitProperties.getRecoveryWindow());
        AppUser user = users.findByEmail(email);
        if (user == null || user.status() != com.perfumestore.backend.user.UserStatus.ACTIVE) {
            return;
        }
        Instant now = Instant.now();
        tokens.invalidateActiveForUser(user.id(), now);
        String rawToken = TokenSecurity.newToken();
        String tokenHash = TokenSecurity.sha256(rawToken);
        tokens.insert(UUID.randomUUID(), user.id(), tokenHash, now.plus(properties.getTokenTtl()), now);
        String link = UriComponentsBuilder.fromUriString(properties.getFrontendBaseUrl())
                .queryParam("token", rawToken).build().toUriString();
        try {
            emailSender.sendPasswordRecovery(user.email(), link);
        } catch (RuntimeException exception) {
            tokens.deleteByHash(tokenHash);
            LOGGER.warn("Falha no envio de recuperação; a resposta pública permanece genérica");
        }
    }

    @Transactional
    public void reset(String rawToken, String password, String remoteAddress) {
        PasswordPolicy.check(password);
        String tokenHash = TokenSecurity.sha256(rawToken);
        String key = "reset:" + tokenHash + ":" + remoteAddress;
        rateLimiter.check(key, rateLimitProperties.getResetMaxAttempts(), rateLimitProperties.getResetWindow());
        UUID userId = tokens.consume(tokenHash, Instant.now()).orElseThrow(() -> new ApiRequestException(400,
                "PASSWORD_RESET_TOKEN_INVALID", "O token de recuperação é inválido ou expirou."));
        users.changePassword(userId, password);
        tokens.invalidateAllForUser(userId, Instant.now());
        sessions.invalidateAllForPrincipal(userId);
        rateLimiter.clear(key);
    }
}
