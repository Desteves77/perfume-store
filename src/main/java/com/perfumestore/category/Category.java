package com.perfumestore.category;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Table(name = "categories")
@Entity
public class Category {

	@Id	
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(unique = true, nullable = false)
	private String name; 
	
	private String description;
	
	@Column(nullable = false)
	private Boolean active;
	
	@Column(nullable = false)
	private LocalDateTime createdAt;
	
	@Column(nullable = false)
	private LocalDateTime updatedAt;
	
	
public Category() {
	
}

public Category(String name, String description, Boolean active) {
	this.name = name;
	this.description = description;
	this.active = active;
}

public Long getId() {
	return id;
}

public String getName() {
	return name;
}

public String getDescription() {
	return description;
}

public Boolean getActive() {
	return active;
}

public LocalDateTime getCreatedAt() {
	return createdAt;
}

public LocalDateTime getUpdatedAt() {
	return updatedAt;
}

public void setId(Long id) {
	this.id = id;
}

public void setName(String name) {
	this.name = name;
}

public void setDescription(String description) {
	this.description = description;
}

public void setActive(Boolean active) {
	this.active = active;
}

@PrePersist
public void OnCreate() {
	 if (this.active == null) {
	        this.active = true;
	    }
	
	this.createdAt = LocalDateTime.now();
	this.updatedAt = LocalDateTime.now();
}
	

@PreUpdate
public void OnUpdate() {
	this.updatedAt = LocalDateTime.now();
}


	
}
