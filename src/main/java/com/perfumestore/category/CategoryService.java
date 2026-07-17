package com.perfumestore.category;

import java.util.List;

import org.springframework.stereotype.Service;

import com.perfumestore.category.dto.CategoryRequestDTO;
import com.perfumestore.category.dto.CategoryResponseDTO;
import com.perfumestore.common.exception.BusinessException;
import com.perfumestore.common.exception.ResourceNotFoundException;

@Service
public class CategoryService {

	private final CategoryRepository repository;
	
	
public CategoryService(CategoryRepository repository) {
	this.repository = repository;
}


private CategoryResponseDTO toResponseDTO(Category category) {
    return new CategoryResponseDTO(
        category.getId(),
        category.getName(),
        category.getDescription(),
        category.getActive(),
        category.getCreatedAt(),
        category.getUpdatedAt()
    );
}

public CategoryResponseDTO create(CategoryRequestDTO dto) {
	
	if(dto.name() == null || dto.name().isBlank()) {
		throw new BusinessException("Nome da categoria não pode ser vazio");
	}
	
	if(repository.existsByNameIgnoreCase(dto.name())){
		throw new BusinessException("Categoria já existente");
	}
	
	Category category = new Category();
	category.setName(dto.name());
	category.setDescription(dto.description());
	category.setActive(true);
	
	Category saved = repository.save(category);
	
	CategoryResponseDTO response = toResponseDTO(saved);
	
	return response;
	
}

public List<CategoryResponseDTO> findAllActive() {
    return repository.findAllByActiveTrue()
            .stream()
            .map(this::toResponseDTO)
            .toList();
}

public Category findEntityById(Long id) {
	
	Category category = repository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
	
	return category;
}

public CategoryResponseDTO findById(Long id) {
	
	Category category = findEntityById(id);
	
	CategoryResponseDTO response = toResponseDTO(category);
	
	return response;
}

public CategoryResponseDTO update(Long id, CategoryRequestDTO dto) {
	
	Category category = findEntityById(id);
	
	if(dto.name()== null||dto.name().isBlank()) {
		throw new BusinessException("Categoria não pode ser vazia");
	}
	
	if(!(dto.name().equalsIgnoreCase(category.getName()))) {
		if(repository.existsByNameIgnoreCase(dto.name()) == true){
			throw new BusinessException("Categoria já existente");
		}
	}
	
	category.setName(dto.name());
	category.setDescription(dto.description());
	
	Category salvo = repository.save(category);
	
	CategoryResponseDTO response = toResponseDTO(salvo);
	
	return response; 

}

public CategoryResponseDTO deactivate(Long id) {
	
	Category category = findEntityById(id);

	category.setActive(false);
	
	repository.save(category);
	
	CategoryResponseDTO response = toResponseDTO(category);

	return response; 

}

public CategoryResponseDTO activate(Long id) {
	
	Category category = findEntityById(id);
	
	category.setActive(true);
	
	repository.save(category);
	
	CategoryResponseDTO response = toResponseDTO(category);
	
	return response;
	
}




}
