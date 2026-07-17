package com.perfumestore.auth.dto;

public record AuthResponseDTO(	
		String token,
		String type,
		String name,
		String email
		) 
{
}
