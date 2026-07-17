package com.perfumestore.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequestDTO(
		@NotNull
		@NotBlank
		@Size(min = 2, max = 80)
		String name,
		
		@Size(max = 80)
		String description) 
{	
}
  