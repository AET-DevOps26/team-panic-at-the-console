package com.panicattheconsole.userservice.exception;

public class NotAuthenticatedException extends RuntimeException {

    public NotAuthenticatedException() {
        super("Not authenticated");
    }
}
