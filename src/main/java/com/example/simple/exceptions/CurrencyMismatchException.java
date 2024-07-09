package com.example.simple.exceptions;

public class CurrencyMismatchException extends IllegalArgumentException {

    public CurrencyMismatchException() {
        super("Currency mismatch");
    }
}
