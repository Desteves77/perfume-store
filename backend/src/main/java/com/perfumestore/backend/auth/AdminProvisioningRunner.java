package com.perfumestore.backend.auth;

import com.perfumestore.backend.config.AdminProvisioningProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminProvisioningRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminProvisioningRunner.class);
    private final AdminProvisioningProperties properties;
    private final AdminProvisioningService service;

    public AdminProvisioningRunner(AdminProvisioningProperties properties, AdminProvisioningService service) {
        this.properties = properties;
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        service.provisionFirstAdmin(properties);
        LOGGER.info("Provisionamento dedicado do primeiro administrador concluído; nenhum segredo foi registrado");
    }
}
