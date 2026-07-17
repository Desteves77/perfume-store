package com.perfumestore.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.perfumestore.user.User;

@Service
public class TokenService {

	private static final String ISSUER = "estoque-spring-api";;
	
	@Value("${jwt.secret}") 
	private String secret;
	
	@Value("${jwt.expiration-hours}")
	private Long expirationHours;
	
	public String generateToken(User user) {
		Algorithm algorithm = Algorithm.HMAC256(secret);
		
		return JWT.create()
				.withIssuer(ISSUER)
				.withSubject(user.getEmail())
				.withExpiresAt(Instant.now().plus(expirationHours, ChronoUnit.HOURS))
				.sign(algorithm);
	}
	
	public String validateToken(String token) {
		Algorithm algorithm = Algorithm.HMAC256(secret);
		
		return JWT.require(algorithm)
				.withIssuer(ISSUER)
				.build()
				.verify(token)
				.getSubject();
	}

}
