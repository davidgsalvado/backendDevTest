package com.ams.similarproducts.infrastructure.api;

import com.ams.similarproducts.domain.config.ExternalProps;
import com.ams.similarproducts.domain.exception.NotFoundException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetSimilarProductsApiAdapterTest {

    @Mock
    private ExternalProps externalProps;

    private MockWebServer mockWebServer;

    private GetSimilarProductsApiAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        final WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/product").toString())
                .build();

        adapter = new GetSimilarProductsApiAdapter(webClient, externalProps);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getSimilarProductsIds_shouldReturnListOfIds() {
        when(externalProps.getMaxTimeout()).thenReturn(5);

        mockWebServer.enqueue(new MockResponse()
                .setBody("[\"2\",\"3\",\"4\"]")
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(adapter.getSimilarProductsIds("1"))
                .expectNext(List.of("2", "3", "4"))
                .verifyComplete();
    }

    @Test
    void getSimilarProductsIds_shouldReturnNotFoundException_when404() {
        when(externalProps.getMaxTimeout()).thenReturn(5);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(adapter.getSimilarProductsIds("999"))
                .verifyError(NotFoundException.class);
    }

    @Test
    void getSimilarProductsIds_shouldReturnEmptyList() {
        when(externalProps.getMaxTimeout()).thenReturn(5);

        mockWebServer.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(adapter.getSimilarProductsIds("1"))
                .expectNext(List.of())
                .verifyComplete();
    }
}
