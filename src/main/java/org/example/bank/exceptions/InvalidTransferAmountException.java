package org.example.bank.exceptions;

public class InvalidTransferAmountException extends IllegalArgumentException {

    public InvalidTransferAmountException() {
        super("Invalid transfer amount");
    }
}
