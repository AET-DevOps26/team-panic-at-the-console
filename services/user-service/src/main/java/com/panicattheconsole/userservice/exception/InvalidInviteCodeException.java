package com.panicattheconsole.userservice.exception;

public class InvalidInviteCodeException extends RuntimeException {

    public InvalidInviteCodeException() {
        super("Invalid invitation code");
    }
}
