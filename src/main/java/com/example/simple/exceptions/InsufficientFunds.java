package com.example.simple.exceptions;

public class InsufficientFunds extends IllegalArgumentException {

    public InsufficientFunds() {
        super("Insufficient funds");
    }
}
