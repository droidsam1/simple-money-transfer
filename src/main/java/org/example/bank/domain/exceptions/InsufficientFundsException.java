package org.example.bank.domain.exceptions;

public class InsufficientFundsException extends IllegalStateException {

    public InsufficientFundsException() {
        super("Insufficient funds");
    }
}
