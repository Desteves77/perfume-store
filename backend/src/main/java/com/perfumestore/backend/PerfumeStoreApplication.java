package com.perfumestore.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.perfumestore.backend.config.OrderProperties;
import com.perfumestore.backend.config.PaymentProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({OrderProperties.class, PaymentProperties.class})
public class PerfumeStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerfumeStoreApplication.class, args);
    }
}

