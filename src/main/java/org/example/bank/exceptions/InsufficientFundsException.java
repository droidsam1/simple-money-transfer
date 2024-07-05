package org.example.bank.exceptions;

public class InsufficientFundsException extends IllegalArgumentException {

    public InsufficientFundsException() {
        super("Insufficient funds");
    }
}
