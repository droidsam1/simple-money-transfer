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
            introduceDelay();
            if (b.amount().compareTo(amount.amount()) < 0) {
                throw new InsufficientFundsException();
            }
            return new Money(b.amount().subtract(amount.amount()), b.currency());
        });
        return true;
    }

    public boolean compareAndWithdraw(Money currentAmount, Money amount) {
        validateSameCurrency(amount.currency());
        if (currentAmount.amount().compareTo(amount.amount()) < 0) {
            throw new InsufficientFundsException();
        }
        introduceDelay();
        return this.balance.compareAndSet(
                currentAmount,
                new Money(currentAmount.amount().subtract(amount.amount()), currentAmount.currency())
        );
    }

    public boolean compareAndDeposit(Money currentAmount, Money amount) {
        validateSameCurrency(amount.currency());
        introduceDelay();
        return this.balance.compareAndSet(
                currentAmount,
                new Money(currentAmount.amount().add(amount.amount()), currentAmount.currency())
        );
    }


    public boolean deposit(Money amount) {
        validateSameCurrency(amount.currency());
        this.balance.getAndUpdate(b -> {
            introduceDelay();
            return new Money(b.amount().add(amount.amount()), b.currency());
        });
        return true;
    }

    private void validateSameCurrency(Currency currency) {
        if (!balance.get().currency().equals(currency)) {
            throw new UnsupportedOperationException("Different currencies, not supported yet");
        }
    }

    private void introduceDelay() {
        for (int i = 0; i < 1_000_000; i++) {
            // Introduce delay
        }

    }

    public AccountId id() {
        return id;
    }

    public Money balance() {
        return balance.get();
    }

    @Override public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        Account account = (Account) object;
        return Objects.equals(id, account.id) && Objects.equals(balance.get(), account.balance.get());
    }

    @Override public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(balance.get());
        return result;
    }

    @Override
    public String toString() {
        return "Account[" +
               "id=" + id + ", " +
               "balance=" + balance + ']';
    }

}
