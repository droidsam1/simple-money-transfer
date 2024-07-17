package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;

public class Account {

    private final AccountId id;
    private Money balance;

    public Account(String id, Money balance) {
        this.balance = balance;
        this.id = new AccountId(id);
    }

    public Money balance() {
        return balance;
    }

    public void transfer(Account recipient, Money funds) {
        //this should be atomic
        this.withdraw(funds);
        recipient.deposit(funds);
    }

    private void deposit(Money funds) {
        this.balance = this.balance.add(funds);
    }

    private void withdraw(Money funds) {
        validateEnoughFunds(funds);
        this.balance = this.balance.subtract(funds);
    }

    private void validateEnoughFunds(Money funds) {
        if (this.balance.amount().compareTo(funds.amount()) < 0) {
            throw new InsufficientFundsException();
        }
    }
}
