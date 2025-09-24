package com.local_review_deal_sys.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")           // 所有接口
                        .allowedOrigins("http://localhost:8080") // 前端地址
                        .allowedMethods("*")         // GET, POST, PUT, DELETE ...
                        .allowCredentials(true);    // 如果要发送 Cookie
            }
        };
    }
}
