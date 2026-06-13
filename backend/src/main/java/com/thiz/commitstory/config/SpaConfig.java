package com.thiz.commitstory.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.setOrder(Integer.MIN_VALUE + 1)
            .addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(false)
            .addResolver(new ResourceResolver() {
                @Override
                public Resource resolveResource(HttpServletRequest request, String requestPath,
                        List<? extends Resource> locations, ResourceResolverChain chain) {
                    // Don't interfere with API routes
                    if (requestPath.startsWith("api/")) {
                        return chain.resolveResource(request, requestPath, locations);
                    }
                    
                    Resource resource = chain.resolveResource(request, requestPath, locations);
                    if (resource != null) {
                        return resource;
                    }
                    
                    // Fall back to index.html for SPA routes
                    try {
                        return new ClassPathResource("/static/index.html");
                    } catch (Exception e) {
                        return null;
                    }
                }

                @Override
                public String resolveUrlPath(String resourcePath, List<? extends Resource> locations,
                        ResourceResolverChain chain) {
                    return chain.resolveUrlPath(resourcePath, locations);
                }
            });
    }
}

