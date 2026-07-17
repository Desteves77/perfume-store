package com.perfumestore.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.exceptions.JWTVerificationException;

import com.perfumestore.auth.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityFilter  extends OncePerRequestFilter {

		private final TokenService tokenService;
	    private final CustomUserDetailsService userDetailsService;

	    public SecurityFilter(TokenService tokenService, CustomUserDetailsService userDetailsService) {
	        this.tokenService = tokenService;
	        this.userDetailsService = userDetailsService;
	    }

	    @Override
	    protected void doFilterInternal(
	            HttpServletRequest request,
	            HttpServletResponse response,
	            FilterChain filterChain
	    ) throws ServletException, IOException {

	        String authorizationHeader = request.getHeader("Authorization");

	        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
	            filterChain.doFilter(request, response);
	            return;
	        }

	        String token = authorizationHeader.replace("Bearer ", "");

	        try {
	            String email = tokenService.validateToken(token);

	            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
	                UserDetails user = userDetailsService.loadUserByUsername(email);

	                UsernamePasswordAuthenticationToken authentication =
	                        new UsernamePasswordAuthenticationToken(
	                                user,
	                                null,
	                                user.getAuthorities()
	                        );

	                authentication.setDetails(
	                        new WebAuthenticationDetailsSource().buildDetails(request)
	                );

	                SecurityContextHolder.getContext().setAuthentication(authentication);
	            }

	        } catch (JWTVerificationException exception) {
	            SecurityContextHolder.clearContext();
	        }

	        filterChain.doFilter(request, response);
	    }
}
