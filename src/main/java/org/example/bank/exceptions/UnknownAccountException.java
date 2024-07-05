package org.example.bank.exceptions;

public class UnknownAccountException extends IllegalArgumentException {


    public UnknownAccountException() {
        super("Account not found");
    }
}
