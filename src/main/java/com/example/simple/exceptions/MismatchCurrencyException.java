package com.example.simple.exceptions;

public class MismatchCurrencyException extends IllegalArgumentException {

    public MismatchCurrencyException() {
        super("Mismatch currency");
    }
}
