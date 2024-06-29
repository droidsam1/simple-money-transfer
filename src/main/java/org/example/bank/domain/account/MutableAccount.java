package org.example.bank.domain.account;

import org.example.bank.domain.exceptions.BalanceMisMatchException;
import org.example.bank.domain.exceptions.InsufficientFundsException;
import org.example.bank.domain.money.Money;

public final class MutableAccount implements Account {

    private final AccountId id;
    private Money balance;

    public MutableAccount(AccountId id, Money balance) {
        this.id = id;
        this.balance = balance;
    }

    public MutableAccount(String id, Money balance) {
        this(new AccountId(id), balance);
    }

    @Override
    public MutableAccount withdraw(Money amount) {
        validateEnoughFunds(amount);
        this.balance = this.balance.subtract(amount);
        return this;
    }

    private void validateEnoughFunds(Money amount) {
        if (balance.amount().compareTo(amount.amount()) < 0) {
            throw new InsufficientFundsException();
        }
    }

    @Override
    public MutableAccount deposit(Money amount) {
        this.balance = this.balance.add(amount);
        return this;
    }

    @Override
    public AccountId id() {
        return id;
    }

    @Override
    public synchronized void compareAndSubtract(Money originBalance, Money moneyToSubtract) {
        if (!balance.equals(originBalance)) {
            throw new BalanceMisMatchException();
        }
        withdraw(moneyToSubtract);
    }

    @Override
    public synchronized void compareAndAdd(Money originBalance, Money moneyToAdd) {
        if (!balance.equals(originBalance)) {
            throw new BalanceMisMatchException();
        }
        deposit(moneyToAdd);
    }

    public Money balance() {
        return balance;
    }
}
