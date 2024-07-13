package com.example.simple.exceptions;

public class NegativeTransferAmountException extends IllegalArgumentException {

    public NegativeTransferAmountException() {
        super("Negative transfer amount");
    }
}
