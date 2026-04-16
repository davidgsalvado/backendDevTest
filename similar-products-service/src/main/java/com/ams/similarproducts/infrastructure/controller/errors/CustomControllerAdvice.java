package com.ams.similarproducts.infrastructure.controller.errors;

import com.ams.similarproducts.domain.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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

}
