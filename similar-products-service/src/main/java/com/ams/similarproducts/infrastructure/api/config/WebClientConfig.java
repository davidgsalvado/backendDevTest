package com.ams.similarproducts.infrastructure.api.config;

import com.ams.similarproducts.domain.config.ExternalProps;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Component
@RequiredArgsConstructor
public class WebClientConfig {

    private final ExternalProps externalProps;

    @Bean
    @Qualifier("similarProductsWebClient")
    public WebClient similarProductsWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(this.externalProps.getBaseUrl())
                .build();
    }

}
