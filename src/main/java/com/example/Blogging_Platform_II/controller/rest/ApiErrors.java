package com.example.Blogging_Platform_II.controller.rest;

import com.example.Blogging_Platform_II.exception.BlogException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiErrors extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiErrors.class);

    @ExceptionHandler(BlogException.class)
    ResponseEntity<ApiEnvelope<Void>> businessError(BlogException exception) {
        HttpStatus status = switch (exception.code()) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(ApiEnvelope.error(exception.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiEnvelope<Void>> unexpected(Exception exception) {
        LOG.error("REST request failed ({})", exception.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(ApiEnvelope.error("The request could not be completed.", null));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
                                                              HttpStatusCode status, WebRequest request) {
        if (exception instanceof MethodArgumentNotValidException invalid) {
            return super.handleExceptionInternal(exception, ApiEnvelope.error("Check the request fields.",
                    fields(invalid.getBindingResult())), headers, status, request);
        }
        HttpStatus standard = HttpStatus.resolve(status.value());
        String message = standard == null ? "The request could not be completed." : standard.getReasonPhrase();
        return super.handleExceptionInternal(exception, ApiEnvelope.error(message, null), headers, status, request);
    }

    private Map<String, String> fields(BindingResult result) {
        Map<String, String> fields = new LinkedHashMap<>();
        result.getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        result.getGlobalErrors().forEach(error -> fields.putIfAbsent("request", error.getDefaultMessage()));
        return fields;
    }
}
