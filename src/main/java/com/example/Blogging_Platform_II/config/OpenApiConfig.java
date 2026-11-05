
package com.example.Blogging_Platform_II.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI bloggingApi() {
        return new OpenAPI().info(new Info().title("Blogging Platform II").version("1.0")
                        .description("Public content reads and account registration are open. Use Basic authentication "
                                + "for account access, administration, and content changes."))
                .components(new Components().addSecuritySchemes("basicAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")));
    }
}
