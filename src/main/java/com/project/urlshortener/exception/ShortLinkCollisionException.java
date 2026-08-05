package com.project.urlshortener.exception;

public class ShortLinkCollisionException extends RuntimeException {

    public ShortLinkCollisionException(String message) {
        super(message);
    }
}
