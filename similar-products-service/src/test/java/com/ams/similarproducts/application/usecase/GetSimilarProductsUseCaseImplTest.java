package com.ams.similarproducts.application.usecase;

import com.ams.similarproducts.domain.entity.Product;
import com.ams.similarproducts.domain.external.FetchProductsPort;
import com.ams.similarproducts.domain.external.GetSimilarProductsPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetSimilarProductsUseCaseImplTest {

    @Mock
    private FetchProductsPort fetchProductsPort;

    @Mock
    private GetSimilarProductsPort getSimilarProductsPort;

    @InjectMocks
    private GetSimilarProductsUseCaseImpl getSimilarProductsUseCase;

    private static Product buildProduct(String id) {
        return Product.builder()
                .id(id)
                .name("Product " + id)
                .price(BigDecimal.valueOf(10.00))
                .availability(true)
                .build();
    }

    @Test
    void getSimilarProducts_shouldReturnProductList() {
        final String productId = "1";
        final List<String> similarIds = List.of("2", "3");

        when(getSimilarProductsPort.getSimilarProductsIds(productId))
                .thenReturn(Mono.just(similarIds));
        when(fetchProductsPort.fetchProductById("2"))
                .thenReturn(Mono.just(buildProduct("2")));
        when(fetchProductsPort.fetchProductById("3"))
                .thenReturn(Mono.just(buildProduct("3")));

        StepVerifier.create(getSimilarProductsUseCase.getSimilarProducts(productId))
                .expectNextMatches(products -> products.size() == 2)
                .verifyComplete();

        verify(getSimilarProductsPort).getSimilarProductsIds(productId);
        verify(fetchProductsPort).fetchProductById("2");
        verify(fetchProductsPort).fetchProductById("3");
    }

    @Test
    void getSimilarProducts_shouldReturnEmptyListWhenNoSimilarIds() {
        final String productId = "1";

        when(getSimilarProductsPort.getSimilarProductsIds(productId))
                .thenReturn(Mono.just(List.of()));

        StepVerifier.create(getSimilarProductsUseCase.getSimilarProducts(productId))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

        verify(getSimilarProductsPort).getSimilarProductsIds(productId);
        verifyNoInteractions(fetchProductsPort);
    }

    @Test
    void getSimilarProducts_shouldDeduplicateSimilarIds() {
        final String productId = "1";
        final List<String> similarIds = List.of("2", "2", "3");

        when(getSimilarProductsPort.getSimilarProductsIds(productId))
                .thenReturn(Mono.just(similarIds));
        when(fetchProductsPort.fetchProductById("2"))
                .thenReturn(Mono.just(buildProduct("2")));
        when(fetchProductsPort.fetchProductById("3"))
                .thenReturn(Mono.just(buildProduct("3")));

        StepVerifier.create(getSimilarProductsUseCase.getSimilarProducts(productId))
                .expectNextMatches(products -> products.size() == 2)
                .verifyComplete();
    }

    @Test
    void getSimilarProducts_shouldSkipProductOnFetchError() {
        final String productId = "1";
        final List<String> similarIds = List.of("2", "3");

        when(getSimilarProductsPort.getSimilarProductsIds(productId))
                .thenReturn(Mono.just(similarIds));
        when(fetchProductsPort.fetchProductById("2"))
                .thenReturn(Mono.error(new RuntimeException("API error")));
        when(fetchProductsPort.fetchProductById("3"))
                .thenReturn(Mono.just(buildProduct("3")));

        StepVerifier.create(getSimilarProductsUseCase.getSimilarProducts(productId))
                .expectNextMatches(products -> products.size() == 1
                        && "3".equals(products.get(0).getId()))
                .verifyComplete();
    }

    @Test
    void getSimilarProducts_shouldPropagateErrorFromGetSimilarProductsPort() {
        final String productId = "1";

        when(getSimilarProductsPort.getSimilarProductsIds(productId))
                .thenReturn(Mono.error(new RuntimeException("upstream error")));

        StepVerifier.create(getSimilarProductsUseCase.getSimilarProducts(productId))
                .verifyError(RuntimeException.class);
    }
}
