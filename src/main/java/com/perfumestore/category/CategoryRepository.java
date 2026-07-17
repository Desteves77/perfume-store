package com.perfumestore.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	boolean existsByNameIgnoreCase(String name);
	
	Optional<Category> findByNameIgnoreCase(String name);
	
	List<Category> findAllByActiveTrue();
	
	
	
}
