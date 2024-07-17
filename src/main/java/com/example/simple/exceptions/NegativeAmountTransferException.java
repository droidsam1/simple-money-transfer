package com.example.simple.exceptions;

public class NegativeAmountTransferException extends IllegalArgumentException {

    public NegativeAmountTransferException() {
        super("Negative amount transfer are not allowed");
    }
}
