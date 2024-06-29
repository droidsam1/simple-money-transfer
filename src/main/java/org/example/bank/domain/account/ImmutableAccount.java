package org.example.bank.domain.account;

import org.example.bank.domain.money.Money;

public record ImmutableAccount(AccountId id, Money balance) implements Account {

    public ImmutableAccount(String id) {
        this(new AccountId(id), new Money(0, "EUR"));
    }

    public ImmutableAccount(String id, Money balance) {
        this(new AccountId(id), balance);
    }

    public ImmutableAccount withdraw(Money amount) {
        return new ImmutableAccount(
                id(),
                new Money(this.balance.amount().subtract(amount.amount()), this.balance.currency())
        );
    }

    public ImmutableAccount deposit(Money amount) {
        return new ImmutableAccount(
                id(),
                new Money(this.balance.amount().add(amount.amount()), this.balance.currency())
        );
    }

    @Override public void compareAndSubtract(Money originBalance, Money moneyToSubtract) {
        throw new UnsupportedOperationException("ImmutableAccount does not support compareAndSubtract");
    }

    @Override public void compareAndAdd(Money originBalance, Money moneyToAdd) {
        throw new UnsupportedOperationException("ImmutableAccount does not support compareAndAdd");
    }

}
