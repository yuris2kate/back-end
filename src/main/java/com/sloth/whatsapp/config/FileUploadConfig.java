package com.sloth.whatsapp.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import jakarta.servlet.MultipartConfigElement;

@Configuration
public class FileUploadConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        
        // Configurações explícitas
        factory.setMaxFileSize(DataSize.ofMegabytes(80));  // 80MB por arquivo
        factory.setMaxRequestSize(DataSize.ofMegabytes(160)); // 160MB total
        
        // Local temporário (opcional, mas recomendado)
        factory.setLocation(System.getProperty("java.io.tmpdir"));
        
        return factory.createMultipartConfig();
    }
}
