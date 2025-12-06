package org.delcom.app.configs;

import org.delcom.app.interceptors.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

        @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**") // Terapkan ke semua endpoint /api
                .excludePathPatterns("/api/auth/**") // Kecuali endpoint auth
                .excludePathPatterns("/api/public/**"); // Dan endpoint public
    }

        // TAMBAHKAN INI
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = "file:" + uploadDir + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(path);
    }
    
}