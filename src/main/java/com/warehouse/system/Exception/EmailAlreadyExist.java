package com.warehouse.system.Exception;

public class EmailAlreadyExist extends RuntimeException{
    public EmailAlreadyExist(String email) {
        super("An account with this email already exists. Please log in.");
    }
}
