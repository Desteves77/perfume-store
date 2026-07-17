package com.perfumestore.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.perfumestore.category.Category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

@Table(name = "products")
@Entity
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false,  length = 150)
	private String name;
	
	@Column(nullable = false)
	private String description;
	
	@Column(nullable = false)
	private String brand;
	
	@DecimalMin(value = "0.0", inclusive = false)
	@Column(nullable = false)
	private BigDecimal price;
	
	@Min(value = 0)
	@Column(nullable = false)
	private Integer stockQuantity;
	
	private String imageUrl;
	
	@Column(nullable = false)
	private Boolean active;
	
	@ManyToOne
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;
	
	@Column(nullable = false)
	private LocalDateTime createdAt;
	
	@Column(nullable = false)
	private LocalDateTime updatedAt;
	
public Product() {
	
}

public Product(String name, String description, String brand, BigDecimal price,
			   Integer stockQuantity, Category category) {
	this.name = name;
	this.description = description;
	this.brand = brand;
	this.price = price;
	this.stockQuantity = stockQuantity;
	this.category = category;
	
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

public String getBrand() {
	return brand;
}

public BigDecimal getPrice() {
	return price;
}

public Integer getStockQuantity() {
	return stockQuantity;
}

public String getImageUrl() {
	return imageUrl;
}

public Boolean getActive() {
	return active;
}

public Category getCategory() {
	return category;
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

public void setBrand(String brand) {
	this.brand = brand;
}

public void setPrice(BigDecimal price) {
	this.price = price;
}

public void setStockQuantity(Integer stockQuantity) {
	this.stockQuantity = stockQuantity;
}

public void setImageUrl(String imageUrl) {
	this.imageUrl = imageUrl;
}

public void setActive(Boolean active) {
	this.active = active;
}

public void setCategory(Category category) {
	this.category = category;
}

public void setCreatedAt(LocalDateTime createdAt) {
	this.createdAt = createdAt;
}

public void setUpdatedAt(LocalDateTime updatedAt) {
	this.updatedAt = updatedAt;
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
