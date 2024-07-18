package com.example.simple.exceptions;

public class NegativeTransferAttemptException extends IllegalArgumentException {

    public NegativeTransferAttemptException() {
        super("Negative transfer attempt");
    }
}
