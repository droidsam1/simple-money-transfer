package com.example.simple.exceptions;

public class InsufficientFundsException extends IllegalStateException {

    public InsufficientFundsException() {
        super("Insufficient funds exception");
    }

}
