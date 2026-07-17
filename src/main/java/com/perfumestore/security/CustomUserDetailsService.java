package com.perfumestore.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.perfumestore.user.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository repository;

	public CustomUserDetailsService(UserRepository repository){
		this.repository = repository;
	}
		
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		com.perfumestore.user.User user = repository.findByEmail(email)
			.orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
		
		  return User.builder()
	              .username(user.getEmail())
	              .password(user.getPassword())
	              .roles(user.getRole().name())
	              .build();
	  }
	
}
