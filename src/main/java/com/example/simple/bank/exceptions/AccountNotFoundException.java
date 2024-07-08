package com.example.simple.bank.exceptions;

public class AccountNotFoundException extends IllegalArgumentException {

    public AccountNotFoundException() {
        super("Account not found");
    }
}
