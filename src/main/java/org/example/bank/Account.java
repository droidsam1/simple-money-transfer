package org.example.bank;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.example.bank.exceptions.CurrencyMistMatchException;
import org.example.bank.exceptions.InsufficientFundsException;

public final class Account {

    private final AccountId id;
    private final String name;
    private final AtomicReference<Money> balance;

    public Account(AccountId id, String name, Money balance) {
        this.id = id;
        this.name = name;
        this.balance = new AtomicReference<>(balance);
    }

    public Account(String name, Money balance) {
        this(new AccountId(), name, balance);
    }

    public void withdraw(Money amountToWithdraw) {
        if (this.balance().amount().compareTo(amountToWithdraw.amount()) < 0) {
            throw new InsufficientFundsException();
        }
        validateSameCurrency(amountToWithdraw);

        this.balance.getAndUpdate(currentBalance -> new Money(
                currentBalance.amount()
                              .subtract(amountToWithdraw.amount()),
                currentBalance.currency()
        ));
    }

    private void validateSameCurrency(Money amountToWithdraw) {
        if (this.balance().currency() != amountToWithdraw.currency()) {
            throw new CurrencyMistMatchException();
        }
    }

    public AccountId id() {
        return id;
    }

    public String name() {
        return name;
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
               Objects.equals(this.name, that.name) &&
               Objects.equals(this.balance.get(), that.balance.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, balance);
    }

    @Override
    public String toString() {
        return "Account[" +
               "id=" + id + ", " +
               "name=" + name + ", " +
               "balance=" + balance + ']';
    }

    public void deposit(Money amountToDeposit) {
        validateSameCurrency(amountToDeposit);

        this.balance.getAndUpdate(currentBalance -> new Money(
                currentBalance.amount().add(amountToDeposit.amount()),
                this.balance().currency()
        ));
    }
}
