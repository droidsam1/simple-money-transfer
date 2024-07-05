package org.example.bank.exceptions;

public class AccountAlreadyRegisteredException extends IllegalArgumentException {

    public AccountAlreadyRegisteredException() {
        super("Already registered account");
    }

}
