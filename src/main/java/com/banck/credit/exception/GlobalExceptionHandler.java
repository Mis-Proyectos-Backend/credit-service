package com.banck.credit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Map<String, Object>> handleBusinessException(
            BusinessException ex) {

        Map<String, Object> error = new HashMap<>();

        error.put("status", 400);
        error.put("message", ex.getMessage());
        error.put("timestamp", LocalDateTime.now());

        return Mono.just(error);
    }

}