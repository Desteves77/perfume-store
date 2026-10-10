package com.perfumestore.backend.security;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security")
public class CsrfController {

    @GetMapping("/csrf")
    public CsrfTokenResponse token(CsrfToken token, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return new CsrfTokenResponse(token.getHeaderName(), token.getToken());
    }
}
