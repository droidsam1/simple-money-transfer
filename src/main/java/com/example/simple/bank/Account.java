package com.example.simple.bank;

import com.example.simple.bank.exceptions.InsufficientFundsException;
import java.util.Currency;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class Account {

    private final AccountId id;
    private final AtomicReference<Money> balance;

    public Account(AccountId id, AtomicReference<Money> balance) {
        this.id = id;
        this.balance = balance;
    }

    public Account(String name, Money initialBalance) {
        this(new AccountId(name), new AtomicReference<>(initialBalance));
    }

    public boolean withdraw(Money amount) {
        validateSameCurrency(amount.currency());
        this.balance.getAndUpdate(b -> {
            if (b.amount().compareTo(amount.amount()) < 0) {
                throw new InsufficientFundsException();
            }
            return new Money(b.amount().subtract(amount.amount()), b.currency());
        });
        return true;
    }

    public boolean deposit(Money amount) {
        validateSameCurrency(amount.currency());
        this.balance.getAndUpdate(b -> new Money(b.amount().add(amount.amount()), b.currency()));
        return true;
    }

    private void validateSameCurrency(Currency currency) {
        if (!balance.get().currency().equals(currency)) {
            throw new UnsupportedOperationException("Different currencies, not supported yet");
        }
    }

    public AccountId id() {
        return id;
    }

    public Money balance() {
        return balance.get();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (Account) obj;
        return Objects.equals(this.id, that.id) &&
               Objects.equals(this.balance, that.balance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, balance);
    }

    @Override
    public String toString() {
        return "Account[" +
               "id=" + id + ", " +
               "balance=" + balance + ']';
    }

}
