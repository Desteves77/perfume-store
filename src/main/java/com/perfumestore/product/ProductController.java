package com.perfumestore.product;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.perfumestore.product.dto.ProductRequestDTO;
import com.perfumestore.product.dto.ProductResponseDTO;

import jakarta.validation.Valid;

@RestController
public class ProductController {

	private final ProductService service;
		
public ProductController(ProductService service) {
	this.service = service;
}



@GetMapping("/products")
public ResponseEntity<List<ProductResponseDTO>>	findAllActive(){
	
	List<ProductResponseDTO> response = service.findAllActive();

	return ResponseEntity.ok(response);

}

@GetMapping("/products/{id}")
public ResponseEntity<ProductResponseDTO> findById(@PathVariable Long id){
	
	ProductResponseDTO response = service.findById(id);
	
	return ResponseEntity.ok(response);
	
}

@GetMapping("/products/category/{categoryId}")
public ResponseEntity<List<ProductResponseDTO>> findByCategory(@PathVariable Long categoryId){
	
	List<ProductResponseDTO> response = service.findAllByCategory(categoryId);
	
	return ResponseEntity.ok(response);
}


@PostMapping("/admin/products")
public ResponseEntity<ProductResponseDTO> create (@Valid @RequestBody ProductRequestDTO dto){
	
	ProductResponseDTO response = service.create(dto);
	
	return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

@PutMapping("/admin/products/{id}")
public ResponseEntity<ProductResponseDTO> update(@PathVariable Long id, @Valid @RequestBody ProductRequestDTO dto){
	
	ProductResponseDTO response = service.update(id, dto);
	
	return ResponseEntity.ok(response);
	
}

@PatchMapping("/admin/products/{id}/deactivate")
public ResponseEntity<ProductResponseDTO> deactivate(@PathVariable Long id){
	
	ProductResponseDTO response = service.deactivate(id);
	
	return ResponseEntity.ok(response);
	
}

@PatchMapping("/admin/products/{id}/activate")
public ResponseEntity<ProductResponseDTO> activate(@PathVariable Long id){
	
	ProductResponseDTO response = service.activate(id);
	
	return ResponseEntity.ok(response);
	
}




}
