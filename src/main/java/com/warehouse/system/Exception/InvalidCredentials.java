package com.warehouse.system.Exception;

public class InvalidCredentials extends RuntimeException {
    public InvalidCredentials() {
        super("Invalid email or password");
    }
}
