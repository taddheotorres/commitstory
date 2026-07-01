package com.thiz.prismgit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PrismgitApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrismgitApplication.class, args);
    }
}
