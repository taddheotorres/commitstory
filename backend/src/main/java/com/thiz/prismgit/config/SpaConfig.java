package com.thiz.prismgit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(false)
            .addResolver(new ResourceResolver() {
                @Override
                public Resource resolveResource(HttpServletRequest request, String requestPath,
                        List<? extends Resource> locations, ResourceResolverChain chain) {
                    if (requestPath.contains(".")) {
                        return chain.resolveResource(request, requestPath, locations);
                    }
                    
                    Resource resource = chain.resolveResource(request, requestPath, locations);
                    if (resource != null) {
                        return resource;
                    }
                    
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

