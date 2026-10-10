package com.perfumestore.backend.security;

public record CsrfTokenResponse(String headerName, String token) {
}
