package com.example.simple;

import com.example.simple.exceptions.MisMatchCurrencyException;

public class Account {

    private final AccountId id;
    private final Money balance;

    public Account(AccountId id, Money initialBalance) {
        this.id = id;
        this.balance = initialBalance;
    }

    public Account(String id, Money initialBalance) {
        this(new AccountId(id), initialBalance);
    }

    public AccountId id() {
        return id;
    }

    public Money balance() {
        return balance;
    }

    public void transfer(Account recipient, Money moneyToTransfer) {
        this.withdraw(moneyToTransfer);
        recipient.deposit(moneyToTransfer);
    }

    private void withdraw(Money moneyToWithdraw) {
        validateEnoughFunds(moneyToWithdraw);
        validateSameCurrency(moneyToWithdraw);

        this.balance.subtract(moneyToWithdraw);
    }

    private void validateSameCurrency(Money moneyToWithdraw) {
        if (this.balance.currency() != moneyToWithdraw.currency()) {
            throw new MisMatchCurrencyException();
        }
    }
}
