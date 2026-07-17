package com.perfumestore.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import com.perfumestore.auth.dto.AuthResponseDTO;
import com.perfumestore.auth.dto.LoginRequestDTO;
import com.perfumestore.auth.dto.RegisterRequestDTO;
import com.perfumestore.auth.dto.RegisterResponseDTO;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthService service;
	
	
public AuthController(AuthService service) {
	this.service = service;
}
	
@PostMapping("/register")
public ResponseEntity<RegisterResponseDTO> register (@Valid @RequestBody RegisterRequestDTO dto){
		
	RegisterResponseDTO response = service.register(dto);
		
	return ResponseEntity.status(201).body(response);
}

@PostMapping("/login")
public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto){
	
	AuthResponseDTO response = service.login(dto);
	
	return ResponseEntity.ok().body(response);
}
	
	
}
