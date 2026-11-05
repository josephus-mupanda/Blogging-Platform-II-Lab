
package com.example.Blogging_Platform_II.config;

import graphql.scalars.ExtendedScalars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphQlConfig {
    @Bean
    RuntimeWiringConfigurer graphQlScalars() {
        return wiring -> wiring.scalar(ExtendedScalars.GraphQLLong);
    }
}
