package com.perfumestore.user.dto;

import java.time.LocalDateTime;

public record UserResponseDTO(
		String name,
		String email,
		LocalDateTime createdAt
){
}
