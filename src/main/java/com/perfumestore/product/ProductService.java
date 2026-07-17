package com.perfumestore.product;

import java.util.List;

import org.springframework.stereotype.Service;

import com.perfumestore.category.Category;
import com.perfumestore.category.CategoryService;
import com.perfumestore.common.exception.BusinessException;
import com.perfumestore.common.exception.ResourceNotFoundException;
import com.perfumestore.product.dto.ProductRequestDTO;
import com.perfumestore.product.dto.ProductResponseDTO;

@Service
public class ProductService {
 
	private final ProductRepository repository;
	
	private final CategoryService service;
	
	
	
public ProductService(ProductRepository repository, CategoryService service) {
	this.repository = repository;
	this.service = service;
}

public ProductResponseDTO toResponseDTO (Product product) {
	
	return new ProductResponseDTO(
			product.getId(),
			product.getName(),
			product.getDescription(),
			product.getBrand(),
			product.getPrice(),
			product.getStockQuantity(),
			product.getImageUrl(),
			product.getActive(),
			product.getCategory().getId(),
			product.getCategory().getName(),
			product.getCreatedAt(),
			product.getUpdatedAt());

}

public Category findCategoryById(Long id) {
	
	Category category = service.findEntityById(id);
	
	return category;
}

private Category findActiveCategoryById(Long categoryId) {
    Category category = service.findEntityById(categoryId);

    if (!category.getActive()) {
        throw new BusinessException("Categoria está inativa");
    }

    return category;
}

public ProductResponseDTO create (ProductRequestDTO dto) {
	
	if(repository.existsByNameIgnoreCase(dto.name())) {
		throw new BusinessException("Produto já existente");
	}
	
	Category category = findActiveCategoryById(dto.categoryId());
	
	Product product = new Product(dto.name(), dto.description(), dto.brand(),
					dto.price(), dto.stockQuantity(), category);
	
	product.setImageUrl(dto.imageUrl());
	product.setActive(true);
	
	Product saved = repository.save(product);
	
	ProductResponseDTO response = toResponseDTO(saved);
	
	return response;
	
	
}

public List<ProductResponseDTO> findAllActive() {
    return repository.findAllByActiveTrueAndCategoryActiveTrue()
            .stream()
            .map(this::toResponseDTO)
            .toList();
}

public ProductResponseDTO findById(Long id) {
	
	Product product = repository.findByIdAndActiveTrue(id)
			.orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
	
	ProductResponseDTO response =  toResponseDTO(product);
	
	return response;
}


public Product findByEntityId(Long id) {
	
	Product product = repository.findById(id)
			.orElseThrow(() -> new BusinessException("Produto não encontrado"));
	
	return product;
	
}

public List<ProductResponseDTO> findAllByCategory(Long id) {
    return repository.findAllByCategoryIdAndActiveTrueAndCategoryActiveTrue(id)
            .stream()
            .map(this::toResponseDTO)
            .toList(); 
}

public ProductResponseDTO update(Long id, ProductRequestDTO dto) {
	
	Product product = findByEntityId(id);
	
	if (!product.getName().equalsIgnoreCase(dto.name())
	        && repository.existsByNameIgnoreCase(dto.name())) {
	    throw new BusinessException("Produto já existente");
	}
	
	Category category = findActiveCategoryById(dto.categoryId());
	
	product.setName(dto.name());
	product.setDescription(dto.description());
	product.setBrand(dto.brand());
	product.setPrice(dto.price());
	product.setStockQuantity(dto.stockQuantity());
	product.setImageUrl(dto.imageUrl());
	product.setCategory(category);
	
	Product saved = repository.save(product);
	
	ProductResponseDTO response = toResponseDTO(saved);
	
	return response;
	
}

public ProductResponseDTO deactivate(Long id) {
	
	Product product = findByEntityId(id);
	
	product.setActive(false);
	
	Product saved = repository.save(product);
	
	ProductResponseDTO response = toResponseDTO(saved);
	
	return response;
	
}

public ProductResponseDTO activate(Long id) {
	
	Product product = findByEntityId(id);
	
	product.setActive(true);
	
	Product saved = repository.save(product);
	
	ProductResponseDTO response = toResponseDTO(saved);
	
	return response;
	
	
}








}
