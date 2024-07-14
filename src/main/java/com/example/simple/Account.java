package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;

public class Account {

    private final AccountId id;
    private Money balance;

    public Account(String id, Money initialBalance) {
        this.id = new AccountId(id);
        this.balance = initialBalance;
    }

    public Money balance() {
        return balance;
    }

    public void transfer(Account recipient, Money transferFunds) {

        if (this.id.compareTo(recipient.id) > 0) {
            synchronized (this) {
                synchronized (recipient) {
                    this.withdraw(transferFunds);
                    recipient.deposit(transferFunds);
                }
            }

        } else {
            synchronized (recipient) {
                synchronized (this) {
                    this.withdraw(transferFunds);
                    recipient.deposit(transferFunds);
                }
            }
        }
    }

    private void withdraw(Money transferFunds) {
        validateEnoughFunds(transferFunds);
        this.balance = this.balance.subtract(transferFunds);
    }

    private void deposit(Money transferFunds) {
        this.balance = this.balance.add(transferFunds);
    }

    private void validateEnoughFunds(Money transferFunds) {

        if (balance.amount().compareTo(transferFunds.amount()) < 0) {
            throw new InsufficientFundsException();
        }
    }
}
