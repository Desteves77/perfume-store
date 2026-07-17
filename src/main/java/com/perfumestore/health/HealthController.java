package com.perfumestore.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.perfumestore.common.exception.BusinessException;
import com.perfumestore.common.exception.ResourceNotFoundException;

@RestController
public class HealthController {

	@GetMapping("/health")
	public ResponseEntity<String> health(){
		return ResponseEntity.ok("API is running");
	}
	
	 @GetMapping("/health/error-business")
	    public ResponseEntity<Void> testBusinessError() {
	        throw new BusinessException("Erro de regra de negócio testado com sucesso");
	    }

	    @GetMapping("/health/error-not-found")
	    public ResponseEntity<Void> testNotFoundError() {
	        throw new ResourceNotFoundException("Recurso de teste não encontrado");
	    }
	
}
