package com.jypLord.exception.user;

public class NoUserLoginException extends RuntimeException {

    public NoUserLoginException(String message) {
        super(message);
    }
}
