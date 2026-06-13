package com.thiz.commitstory.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.setOrder(Integer.MIN_VALUE + 1)
            .addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(false)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    Resource resource = location.createRelative(resourcePath);
                    if (resource.exists() && resource.isReadable()) {
                        return resource;
                    }
                    return new ClassPathResource("/static/index.html");
                }
            });
    }
}
