package com.perfumestore.auth;

import com.perfumestore.common.exception.BusinessException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.perfumestore.auth.dto.AuthResponseDTO;
import com.perfumestore.auth.dto.LoginRequestDTO;
import com.perfumestore.auth.dto.RegisterRequestDTO;
import com.perfumestore.auth.dto.RegisterResponseDTO;
import com.perfumestore.user.User;
import com.perfumestore.user.UserRepository;
import com.perfumestore.user.UserRole;
import com.perfumestore.user.UserService;

@Service
public class AuthService {
	
	private UserRepository repository;
	
	private UserService service;
	
	private TokenService tokenService;
	
	private PasswordEncoder passwordEncoder;
	
	private AuthenticationManager authenticationManager;
	
	
public AuthService(UserRepository repository, UserService service, TokenService tokenService,
		PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
	this.repository = repository;
	this.service = service;
	this.tokenService=  tokenService;
	this.passwordEncoder = passwordEncoder;
	this.authenticationManager = authenticationManager;
}


public RegisterResponseDTO register(RegisterRequestDTO dto) {
	
	if(dto.email()==null|| dto.email().isBlank()) {
		throw new BusinessException("Email não pode ser vazio");
	}
	
	if(service.existsByEmail(dto.email())) {
		throw new BusinessException("Email já cadastrado");
	}
	
	if(dto.name()==null|| dto.email().isBlank()) {
		throw new BusinessException("Nome não pode ser vazio");
	}
	
	if(dto.name().length()<4) {
		throw new BusinessException("Nome não pode ser menor que 4 caracteres");
	}
	
	if(dto.password()==null|| dto.email().isBlank()) {
		throw new BusinessException("Senha não pode ser vazia");
	}
	
	if(dto.password().length()>8) {
		throw new BusinessException("Senha não pode ser menor que 8 caracteres");
	}
	
	String password = dto.password();
	
	User user = new User(
			dto.name(),
			dto.email(),
			passwordEncoder.encode(password),
			UserRole.CUSTOMER);
	
	User savedUser = repository.save(user);
	
	RegisterResponseDTO response = new RegisterResponseDTO(
									savedUser.getEmail(),
									savedUser.getName());
	return response;
	
}

public AuthResponseDTO login(LoginRequestDTO dto) {
	
	if(dto.email()==null|| dto.email().isBlank()){
		throw new BusinessException("Email não pode ser vazio");
	}
	
	if(dto.password()==null|| dto.email().isBlank()) {
		throw new BusinessException("Senha não pode ser vazia");
	}
	
	UsernamePasswordAuthenticationToken authToken = 
			new UsernamePasswordAuthenticationToken(
					dto.email(),
					dto.password()); 
	
	authenticationManager.authenticate(authToken);
	
	User token = repository.findByEmail(dto.email())
			.orElseThrow(() -> new BusinessException ("Credenciais inválidas")); 
	
	String tokenResponse  = tokenService.generateToken(token);
	
	AuthResponseDTO response = new AuthResponseDTO(
								tokenResponse, "Bearer",
								token.getName(),
								token.getEmail());
	return response;
	
	
}


	
}
