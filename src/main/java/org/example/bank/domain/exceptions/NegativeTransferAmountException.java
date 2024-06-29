package org.example.bank.domain.exceptions;

public class NegativeTransferAmountException extends IllegalArgumentException {

    public NegativeTransferAmountException() {
        super("Amount must be positive");
    }

}
