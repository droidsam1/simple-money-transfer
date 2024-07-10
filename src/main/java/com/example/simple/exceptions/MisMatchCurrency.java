package com.example.simple.exceptions;

public class MisMatchCurrency extends IllegalArgumentException {

    public MisMatchCurrency() {
        super("Mismatch currency");
    }

}
