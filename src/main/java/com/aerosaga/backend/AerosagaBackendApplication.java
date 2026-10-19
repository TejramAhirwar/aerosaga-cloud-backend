package com.aerosaga.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AerosagaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AerosagaBackendApplication.class, args);

    }

}
