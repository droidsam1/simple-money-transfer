package com.example.simple.bank.exceptions;

public class InsufficientFundsException extends IllegalArgumentException {

    public InsufficientFundsException() {
        super("Insufficient funds");
    }
}
