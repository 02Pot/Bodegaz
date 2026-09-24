package com.warehouse.system.Exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
      super("Unauthorized to view this please login");
    }
}
