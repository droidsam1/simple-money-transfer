package org.example.bank.exceptions;

public class NegativeBalanceAccountRegistrationException extends IllegalArgumentException {

    public NegativeBalanceAccountRegistrationException() {
        super("Cannot register clients with negative balance");
    }
}
