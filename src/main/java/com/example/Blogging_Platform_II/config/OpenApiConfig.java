
package com.example.Blogging_Platform_II.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI bloggingApi() {
        return new OpenAPI().info(new Info().title("Blogging Platform II").version("1.0"));
    }
}
