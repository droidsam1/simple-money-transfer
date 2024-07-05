package org.example.bank.exceptions;

public class CurrencyMistMatchException extends IllegalArgumentException {

    public CurrencyMistMatchException() {
        super("Currency mismatch");
    }
}
