package org.example.bank.domain.exceptions;

public class AccountNotFoundException extends IllegalArgumentException {

    public AccountNotFoundException() {
        super("Account not found");
    }
}
