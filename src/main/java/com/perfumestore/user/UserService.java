package com.perfumestore.user;


import org.springframework.stereotype.Service;

import com.perfumestore.common.exception.ResourceNotFoundException;
import com.perfumestore.user.dto.UserResponseDTO;

@Service
public class UserService {
	
	private final UserRepository repository;
	
	
public UserService(UserRepository repository){
	this.repository = repository;
}


public User findByEmail(String email) {
	
	User userEmail = repository.findByEmail(email)
			.orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
	
	return userEmail;
	
}
	
public User findById (Long id) {
	User userId = repository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
	
	return userId;
	
}

public boolean existsByEmail(String email) {
	return repository.existsByEmail(email);
}


public UserResponseDTO toResponseDTO(User user) {
	
	UserResponseDTO dto  = new UserResponseDTO(
			user.getName(),
			user.getEmail(),
			user.getCreatedAt());
	
	return dto;
}

	
}
