package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;

public class Account {

    private final AccountId id;
    private Money balance;

    public Account(String id, Money balance) {
        this(new AccountId(id), balance);
    }

    public Account(AccountId id, Money balance) {
        this.id = id;
        this.balance = balance;
    }

    public Money balance() {
        return this.balance;
    }

    public void withdraw(Money funds) {
        validateEnoughFunds(funds);
        this.balance = this.balance.subtract(funds);
    }

    private void validateEnoughFunds(Money funds) {
        if (this.balance.amount().compareTo(funds.amount()) < 0) {
            throw new InsufficientFundsException();
        }
    }

    public void deposit(Money funds) {
        this.balance = this.balance.deposit(funds);
    }

    public void transfer(Account beneficiary, Money funds) {
        if (this.id.compareTo(beneficiary.id) > 0) {
            synchronized (this) {
                synchronized (beneficiary) {
                    this.withdraw(funds);
                    beneficiary.deposit(funds);
                }
            }
        } else {
            synchronized (beneficiary) {
                synchronized (this) {
                    this.withdraw(funds);
                    beneficiary.deposit(funds);
                }
            }
        }
    }
}
