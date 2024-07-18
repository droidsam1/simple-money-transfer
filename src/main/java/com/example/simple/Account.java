package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import com.example.simple.exceptions.NegativeTransferAttemptException;
import java.math.BigDecimal;

public class Account {

    private final AccountId id;
    private Money balance;

    public Account(String id, Money balance) {
        this.id = new AccountId(id);
        this.balance = balance;
    }

    public Money balance() {
        return balance;
    }

    public void transfer(Account recipient, Money funds) {
        validateFundsArePositive(funds);
        //this should be atomic

        if (this.id.compareTo(recipient.id) > 0) {
            synchronized (this) {
                synchronized (recipient) {
                    this.withdraw(funds);
                    recipient.deposit(funds);
                }
            }
        } else {
            synchronized (recipient) {
                synchronized (this) {
                    this.withdraw(funds);
                    recipient.deposit(funds);
                }
            }
        }
    }

    private void deposit(Money funds) {
        this.balance = this.balance().add(funds);
    }

    private void withdraw(Money funds) {
        validateEnoughFunds(funds);
        this.balance = this.balance().subtract(funds);
    }

    private void validateEnoughFunds(Money funds) {
        if (this.balance().amount().compareTo(funds.amount()) < 0) {
            throw new InsufficientFundsException();
        }
    }

    private void validateFundsArePositive(Money funds) {
        if (funds.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeTransferAttemptException();
        }
    }
}
