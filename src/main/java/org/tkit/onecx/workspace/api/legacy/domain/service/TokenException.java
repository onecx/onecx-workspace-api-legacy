package org.tkit.onecx.workspace.api.legacy.domain.service;

public class TokenException extends RuntimeException {

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable t) {
        super(message, t);
    }
}
