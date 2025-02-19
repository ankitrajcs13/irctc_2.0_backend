package com.irctc2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    // Defining RestTemplate as a Spring Bean
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}