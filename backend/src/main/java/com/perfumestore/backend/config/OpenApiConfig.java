package com.perfumestore.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "PerfumeStore API",
        version = "v1",
        description = "Contratos técnicos e endpoints implementados do backend PerfumeStore."))
public class OpenApiConfig {
}
