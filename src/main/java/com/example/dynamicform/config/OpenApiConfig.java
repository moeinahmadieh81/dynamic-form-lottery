package com.example.dynamicform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI dynamicFormLotteryOpenApi() {

        SecurityScheme bearerSecurityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name(HttpHeaders.AUTHORIZATION);

        return new OpenAPI()
                .info(new Info()
                        .title("Dynamic Form Lottery API")
                        .description("""
                                REST API for dynamic form management,
                                form submissions and lottery execution.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Formino")))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_AUTH,
                                bearerSecurityScheme
                        ))
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(BEARER_AUTH)
                );
    }
}