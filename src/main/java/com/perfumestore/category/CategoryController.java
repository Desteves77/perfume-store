package com.perfumestore.category;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.perfumestore.category.dto.CategoryRequestDTO;
import com.perfumestore.category.dto.CategoryResponseDTO;

import jakarta.validation.Valid;

@RestController
public class CategoryController {

	private final CategoryService service;
	

	
	
public CategoryController(CategoryService service){
	this.service = service;
}

@GetMapping("/categories")
public ResponseEntity<List<CategoryResponseDTO>> findCategory(){
	 
	List<CategoryResponseDTO> response = service.findAllActive();
	 
	 return ResponseEntity.ok(response);
}

@GetMapping("/categories/{id}")
public ResponseEntity<CategoryResponseDTO> findCategoryById(@PathVariable Long id){
	
	CategoryResponseDTO response = service.findById(id);
	
	return ResponseEntity.ok(response);
}

@PostMapping("/admin/categories")   
public ResponseEntity<CategoryResponseDTO> create(@Valid @RequestBody CategoryRequestDTO dto){
	
	CategoryResponseDTO response = service.create(dto);
	
	return ResponseEntity.status(HttpStatus.CREATED).body(response);
	
}

@PutMapping("/admin/categories/{id}")
public ResponseEntity<CategoryResponseDTO> update(@PathVariable Long id,  @Valid @RequestBody CategoryRequestDTO dto){
	
	CategoryResponseDTO response = service.update(id, dto);
	
	return ResponseEntity.ok(response);
	
}

@PatchMapping("/admin/categories/{id}/deactivate")
public ResponseEntity<CategoryResponseDTO> deactivate(@PathVariable Long id){
	
	CategoryResponseDTO response = service.deactivate(id);
	
	return ResponseEntity.ok(response);  
}

@PatchMapping("/admin/categories/{id}/activate")
public ResponseEntity<CategoryResponseDTO> activate(@PathVariable Long id){
	
	CategoryResponseDTO response = service.activate(id);
	
	return ResponseEntity.ok(response);
}
 
     
   
 
	

}
