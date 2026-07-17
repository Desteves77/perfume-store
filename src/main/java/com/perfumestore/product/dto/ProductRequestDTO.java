package com.perfumestore.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequestDTO(
		@NotBlank
		@Size(min = 2, max = 150)
		String name,
		
		@Size(max = 1000)
		String description,
		
		@NotBlank
		@Size(min =2, max = 100)
		String brand,
		
		@NotNull
		@DecimalMin(value = "0.0", inclusive = false)
		BigDecimal price,
		
		@NotNull
		@Min(0)
		Integer stockQuantity,
		
		@Size(max = 500)
		String imageUrl,
		
		@NotNull
		Long categoryId) {

}
