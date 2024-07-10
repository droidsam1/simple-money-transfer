package com.example.simple.exceptions;

public class InsufficientFundsException extends IllegalArgumentException {

    public InsufficientFundsException() {
        super("Insufficient funds");
    }

}
