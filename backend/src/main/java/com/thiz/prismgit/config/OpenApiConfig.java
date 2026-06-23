package com.thiz.prismgit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PrismGit API")
                        .version("1.0.0")
                        .description("Transform Git commit history into narrative stories. Data storytelling dashboard that analyzes commits and generates insights.")
                        .contact(new Contact()
                                .name("Thiz")
                                .url("https://github.com/thiz"))
                        .license(new License()
                                .name("MIT")));
    }
}
