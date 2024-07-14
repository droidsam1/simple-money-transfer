package com.example.simple.exceptions;

public class MisMatchCurrencyException extends IllegalArgumentException {

    public MisMatchCurrencyException() {
        super("MisMatch currency");
    }
}
