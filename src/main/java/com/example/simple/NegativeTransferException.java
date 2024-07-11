package com.example.simple;

public class NegativeTransferException extends IllegalArgumentException {

    public NegativeTransferException() {
        super("Transfer cannot be negative");
    }

}
