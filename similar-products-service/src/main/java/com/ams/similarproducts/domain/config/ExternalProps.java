package com.ams.similarproducts.domain.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@ConfigurationProperties(prefix = "external")
public class ExternalProps {

    private final ProductProps products = new ProductProps();

    private final ConfigProps config = new ConfigProps();

    public String getBaseUrl() {
        return this.products.getBaseUrl();
    }

    public Integer getMaxTimeout() {
        return this.config.getMaxTimeout();
    }

}
