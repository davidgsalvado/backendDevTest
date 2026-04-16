package com.ams.similarproducts.domain.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {

    private final String content;

    public NotFoundException(String content) {
        super(content);
        this.content = content;
    }

}