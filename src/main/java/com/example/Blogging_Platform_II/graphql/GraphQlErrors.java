
package com.example.Blogging_Platform_II.graphql;

import com.example.Blogging_Platform_II.api.ApiException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(annotations = Controller.class)
public class GraphQlErrors {
    @GraphQlExceptionHandler
    GraphQLError business(ApiException exception, DataFetchingEnvironment environment) {
        return error(environment, exception.status().name(), exception.getMessage());
    }

    @GraphQlExceptionHandler
    GraphQLError invalid(ConstraintViolationException exception, DataFetchingEnvironment environment) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .sorted().collect(Collectors.joining(", "));
        return error(environment, "BAD_REQUEST", message);
    }

    @GraphQlExceptionHandler
    GraphQLError invalidArguments(BindException exception, DataFetchingEnvironment environment) {
        return error(environment, "BAD_REQUEST", "Check the argument types and values.");
    }

    @GraphQlExceptionHandler
    GraphQLError duplicate(DuplicateKeyException exception, DataFetchingEnvironment environment) {
        return error(environment, "CONFLICT", "A record already uses this value.");
    }

    private GraphQLError error(DataFetchingEnvironment environment, String code, String message) {
        return GraphqlErrorBuilder.newError(environment).message(message).extensions(Map.of("code", code)).build();
    }
}
