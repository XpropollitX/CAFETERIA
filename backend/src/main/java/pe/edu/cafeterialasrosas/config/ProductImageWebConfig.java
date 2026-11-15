package pe.edu.cafeterialasrosas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pe.edu.cafeterialasrosas.service.ProductImageStorageService;

@Configuration
public class ProductImageWebConfig implements WebMvcConfigurer {

    private final ProductImageStorageService productImageStorageService;

    public ProductImageWebConfig(ProductImageStorageService productImageStorageService) {
        this.productImageStorageService = productImageStorageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/product-images/**")
            .addResourceLocations(productImageStorageService.resourceLocation());
    }
}
