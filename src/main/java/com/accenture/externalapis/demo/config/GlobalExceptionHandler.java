package com.accenture.externalapis.demo.config;

import com.accenture.externalapis.demo.client.ClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Every ClientException becomes 502, even an upstream 404 or 429.
    // This seems to be intended by the assignment requirements, so not changed.
    @ExceptionHandler(ClientException.class)
    public ProblemDetail handleClientException(ClientException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ProblemDetail handleNotImplemented(UnsupportedOperationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED, ex.getMessage());
    }
}