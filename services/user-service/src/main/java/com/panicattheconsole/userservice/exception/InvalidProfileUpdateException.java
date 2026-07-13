package com.panicattheconsole.userservice.exception;

public class InvalidProfileUpdateException extends RuntimeException {

    public InvalidProfileUpdateException(String message) {
        super(message);
    }
}
