package org.example.bank.domain.account;

import java.util.concurrent.atomic.AtomicReference;
import org.example.bank.domain.money.Money;
import org.example.bank.domain.exceptions.BalanceMisMatchException;

public final class MutableAccount implements Account {

    private final AccountId id;
    private final AtomicReference<Money> balance;

    public MutableAccount(AccountId id, Money balance) {
        this.id = id;
        this.balance = new AtomicReference<>(balance);
    }

    public MutableAccount(String id) {
        this(new AccountId(id), new Money(0, "EUR"));
    }

    public MutableAccount(String id, Money balance) {
        this(new AccountId(id), balance);
    }

    public MutableAccount withdraw(Money amount) {
        this.balance.getAndUpdate(b -> b.subtract(amount));
        return this;
    }

    public MutableAccount deposit(Money amount) {
        this.balance.getAndUpdate(b -> b.add(amount));
        return this;
    }

    public AccountId id() {
        return id;
    }

    @Override public void compareAndSetBalance(Money originBalance, Money newValue) {
        if (!balance.get().currency().equals(originBalance.currency())) {
            throw new BalanceMisMatchException();
        }
        this.balance.compareAndSet(originBalance, newValue);
    }

    public Money balance() {
        return balance.get();
    }
}
