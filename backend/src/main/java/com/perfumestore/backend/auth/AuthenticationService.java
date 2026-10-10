package com.perfumestore.backend.auth;

import java.time.Instant;

import com.perfumestore.backend.config.RateLimitProperties;
import com.perfumestore.backend.config.SessionCookieProperties;
import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.user.EmailNormalizer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseCookie;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final CsrfTokenRepository csrfTokenRepository;
    private final UserService users;
    private final AttemptRateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;
    private final SessionCookieProperties sessionCookieProperties;

    public AuthenticationService(AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            CsrfTokenRepository csrfTokenRepository, UserService users, AttemptRateLimiter rateLimiter,
            RateLimitProperties rateLimitProperties, SessionCookieProperties sessionCookieProperties) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.csrfTokenRepository = csrfTokenRepository;
        this.users = users;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
        this.sessionCookieProperties = sessionCookieProperties;
    }

    public AuthDtos.UserResponse login(String email, String password, HttpServletRequest request,
            HttpServletResponse response) {
        String normalized = EmailNormalizer.normalize(email);
        String key = "login:" + normalized + ":" + remoteAddress(request);
        rateLimiter.check(key, rateLimitProperties.getLoginMaxAttempts(), rateLimitProperties.getLoginWindow());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(normalized, password));
        } catch (AuthenticationException exception) {
            throw new ApiRequestException(401, "INVALID_CREDENTIALS",
                    "E-mail ou senha inválidos.");
        }
        rateLimiter.clear(key);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        securityContextRepository.saveContext(context, request, response);
        csrfTokenRepository.saveToken(csrfTokenRepository.generateToken(request), request, response);
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return AuthDtos.UserResponse.from(users.findRequired(principal.id()));
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        csrfTokenRepository.saveToken(null, request, response);
        response.addHeader("Set-Cookie", clearSessionCookie());
        SecurityContextHolder.clearContext();
    }

    public AuthDtos.UserResponse current(AppUserPrincipal principal) {
        return AuthDtos.UserResponse.from(users.findRequired(principal.id()));
    }

    private String remoteAddress(HttpServletRequest request) {
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private String clearSessionCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("SESSION", "")
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .sameSite(sessionCookieProperties.getSameSite());
        if (sessionCookieProperties.isSecure()) {
            builder.secure(true);
        }
        return builder.build().toString();
    }
}
