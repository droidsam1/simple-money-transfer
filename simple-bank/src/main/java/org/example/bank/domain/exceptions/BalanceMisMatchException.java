package org.example.bank.domain.exceptions;

public class BalanceMisMatchException extends IllegalArgumentException {

    public BalanceMisMatchException() {
        super("Balance mismatch");
    }

}
