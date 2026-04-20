package com.ams.similarproducts.infrastructure.controller.errors;

import com.ams.similarproducts.domain.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.concurrent.TimeoutException;

@ControllerAdvice
@Slf4j
public class CustomControllerAdvice {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<CustomErrorResponse> handleNotFoundException(NotFoundException ex) {
        log.error("[CustomControllerAdvice::handleNotFoundException] error received: {}", ex.getMessage());
        final HttpStatus status = HttpStatus.NOT_FOUND; // 404
        final CustomErrorResponse errorResponse = CustomErrorResponse.builder()
                .code("404")
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<CustomErrorResponse> handleWebClientRequestException(WebClientRequestException ex) {
        log.error("[CustomControllerAdvice::handleWebClientRequestException] error received: {}", ex.getMessage());
        final HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE; // 503
        final CustomErrorResponse errorResponse = CustomErrorResponse.builder()
                .code("503")
                .message("Service unavailable: upstream API is not reachable")
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<CustomErrorResponse> handleTimeoutException(TimeoutException ex) {
        log.error("[CustomControllerAdvice::handleTimeoutException] error received: {}", ex.getMessage());
        final HttpStatus status = HttpStatus.GATEWAY_TIMEOUT; // 504
        final CustomErrorResponse errorResponse = CustomErrorResponse.builder()
                .code("504")
                .message("Gateway timeout: upstream API did not respond in time")
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }

}
