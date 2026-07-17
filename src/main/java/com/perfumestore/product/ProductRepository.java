package com.perfumestore.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
	
	boolean existsByNameIgnoreCase(String name);
	
	List<Product> findAllByActiveTrueAndCategoryActiveTrue();	
	 
    Optional<Product> findByIdAndActiveTrue(Long id);
    
	
    List<Product> findAllByCategoryIdAndActiveTrueAndCategoryActiveTrue(Long categoryId);

	
}
	