package com.ams.similarproducts.infrastructure.controller.errors;

import com.ams.similarproducts.domain.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.net.URI;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class CustomControllerAdviceTest {

    private final CustomControllerAdvice controllerAdvice = new CustomControllerAdvice();

    @Test
    void handleNotFoundException_shouldReturn404WithErrorResponse() {
        final NotFoundException exception = new NotFoundException("Product not found");

        final ResponseEntity<CustomErrorResponse> response = controllerAdvice.handleNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("404", response.getBody().getCode());
        assertEquals("Product not found", response.getBody().getMessage());
    }

    @Test
    void handleWebClientRequestException_shouldReturn503() {
        final WebClientRequestException exception = new WebClientRequestException(
                new java.net.ConnectException("Connection refused"),
                org.springframework.http.HttpMethod.GET,
                URI.create("http://localhost:3001/product/1"),
                org.springframework.http.HttpHeaders.EMPTY
        );

        final ResponseEntity<CustomErrorResponse> response = controllerAdvice.handleWebClientRequestException(exception);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("503", response.getBody().getCode());
        assertEquals("Service unavailable: upstream API is not reachable", response.getBody().getMessage());
    }

    @Test
    void handleTimeoutException_shouldReturn504() {
        final TimeoutException exception = new TimeoutException("Timeout after 5s");

        final ResponseEntity<CustomErrorResponse> response = controllerAdvice.handleTimeoutException(exception);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("504", response.getBody().getCode());
        assertEquals("Gateway timeout: upstream API did not respond in time", response.getBody().getMessage());
    }
}
