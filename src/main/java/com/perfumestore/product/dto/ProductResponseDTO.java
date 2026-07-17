package com.perfumestore.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponseDTO(
		
		Long id,
		String name,
		String description,
		String brand,
		BigDecimal Price,
		Integer stockQuantity,
		String imageUrl,
		Boolean active,
		Long categoryId,
		String categoryName,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

}
