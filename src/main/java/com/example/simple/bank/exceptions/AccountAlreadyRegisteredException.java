package com.example.simple.bank.exceptions;

public class AccountAlreadyRegisteredException extends IllegalArgumentException {

    public AccountAlreadyRegisteredException() {
        super("Account already registered");
    }

}
