package com.perfumestore.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO (
		
		@NotBlank
		@Size(min = 4)
		String name,
		
		@Email
		String email,
		
		@NotBlank
		@Size(min = 8)
		String password)

{
}
