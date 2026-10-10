package com.perfumestore.backend.auth;

import com.perfumestore.backend.config.AdminProvisioningProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AdminProvisioningRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminProvisioningRunner.class);
    private final AdminProvisioningProperties properties;
    private final AdminProvisioningService service;
    private final ConfigurableApplicationContext context;
    private final Environment environment;

    public AdminProvisioningRunner(AdminProvisioningProperties properties, AdminProvisioningService service,
            ConfigurableApplicationContext context, Environment environment) {
        this.properties = properties;
        this.service = service;
        this.context = context;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        service.provisionFirstAdmin(properties);
        if (environment != null && environment.acceptsProfiles("provision-admin") && context != null) {
            System.exit(SpringApplication.exit(context));
        }
        LOGGER.info("Provisionamento dedicado do primeiro administrador concluído; nenhum segredo foi registrado");
    }
}
