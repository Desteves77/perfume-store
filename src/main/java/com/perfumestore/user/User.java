package com.perfumestore.user;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Builder;


@Builder
@Entity

@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false, length = 100)
	private String name;
	
	@Column(nullable = false, unique = true, length = 150)
	private String email;
	
	@Column(nullable = false)
	private String password;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole role; 
	
	@Column(nullable = false)
	private LocalDateTime createdAt;
	
	@Column(nullable = false)
	private LocalDateTime updatedAt;
	
public User() {
	
}

public User(String name, String email, String password, UserRole role) {
	this.name = name; 
	this.email = email;
	this.password = password; 
	this.role = role; 
}

public Long getId() {
	return id;
}

public String getName() {
	return name;
}

public String getEmail() {
	return email;
}

public String getPassword() {
	return password; 
}

public UserRole getRole() {
	return role;
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
	this.name= name;
}

public void setEmail(String email) {
	this.email = email;
}

public void setPassword(String password) {
	this.password= password;
}

public void setRole(UserRole role) {
	this.role = role;
}

public void setCreatedAt(LocalDateTime createdAt) {
	this.createdAt = createdAt;
}

public void setUpdatedAt(LocalDateTime updatedAt) {
	this.updatedAt = updatedAt;
}



@PrePersist
public void OnCreate() {
	this.createdAt = LocalDateTime.now();
	this.updatedAt = LocalDateTime.now();
}
	

@PreUpdate
public void OnUpdate() {
	this.updatedAt = LocalDateTime.now();
}

}
