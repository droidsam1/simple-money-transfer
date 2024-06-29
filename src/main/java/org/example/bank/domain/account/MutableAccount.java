package org.example.bank.domain.account;

import org.example.bank.domain.exceptions.BalanceMisMatchException;
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

    public MutableAccount withdraw(Money amount) {
        this.balance = this.balance.subtract(amount);
        return this;
    }

    public MutableAccount deposit(Money amount) {
        this.balance = this.balance.add(amount);
        return this;
    }

    public AccountId id() {
        return id;
    }

    @Override public synchronized void compareAndSetBalance(Money originBalance, Money newValue) {
        if (!balance.equals(originBalance)) {
            throw new BalanceMisMatchException();
        }
        this.balance = newValue;
    }

    public Money balance() {
        return balance;
    }
}
