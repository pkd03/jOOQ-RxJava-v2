package com.example.pkdv3.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình OpenAPI 3 / Swagger cho hệ thống pkdv3.
 *
 * Mục đích:
 * Tự động sinh tài liệu hóa REST API chuẩn OpenAPI và giao diện Swagger UI tương tác.
 * Giúp lập trình viên và các hệ thống bên ngoài dễ dàng kiểm thử các endpoints.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Định nghĩa Metadata cho tài liệu API tổng thể của ứng dụng.
     */
    @Bean
    public OpenAPI enterpriseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PKD V3 Enterprise REST API")
                        .version("1.0.0")
                        .description("Hệ thống quản lý Product và Category xây dựng trên kiến trúc Spring Boot 3, jOOQ Type-Safe SQL và RxJava 3 Reactive Pipeline")
                        .contact(new Contact()
                                .name("Senior Java Architect Team")
                                .email("architect@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
