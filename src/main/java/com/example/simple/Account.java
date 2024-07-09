package com.example.simple;

import com.example.simple.exceptions.CurrencyMismatchException;
import com.example.simple.exceptions.InsufficientFunds;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class Account {

    private final String id;
    private final AtomicReference<Money> balance;

    public Account(String id, Money balance) {
        this.id = id;
        this.balance = new AtomicReference<>(balance);
    }

    public void transfer(Account to, Money moneyToTransfer) {
        if (balance().amount().compareTo(moneyToTransfer.amount()) < 0) {
            throw new InsufficientFunds();
        }
        if (!balance().currency().equals(moneyToTransfer.currency())) {
            throw new CurrencyMismatchException();
        }
        //optimistic approach
        while (true) {
            Money expectedBalance = this.balance();
            Money expectedDestinyBalance = to.balance();

            boolean withdraw;
            boolean deposit;

            withdraw = this.balance.compareAndSet(
                    expectedBalance,
                    new Money(
                            expectedBalance.amount().subtract(moneyToTransfer.amount()),
                            expectedBalance.currency()
                    )
            );
            if (withdraw) {
                deposit = to.balance.compareAndSet(
                        expectedDestinyBalance,
                        new Money(
                                expectedDestinyBalance.amount().add(moneyToTransfer.amount()),
                                expectedDestinyBalance.currency()
                        )
                );
                if (deposit) {
                    break;
                } else {
                    //rollback: naive implementation
                    this.deposit(moneyToTransfer);
                }
            }
        }
        //pessimistic approach
        //        pessimisticTransfer(to, moneyToTransfer);
    }

    private void pessimisticTransfer(Account to, Money moneyToTransfer) {
        if (this.id().compareToIgnoreCase(to.id()) > 0) {
            synchronized (this) {
                synchronized (to) {
                    this.withdraw(moneyToTransfer);
                    to.deposit(moneyToTransfer);
                }
            }
        } else {
            synchronized (to) {
                synchronized (this) {
                    this.withdraw(moneyToTransfer);
                    to.deposit(moneyToTransfer);
                }
            }
        }
    }

    private void withdraw(Money moneyToTransfer) {
        this.balance.updateAndGet(b -> new Money(b.amount().subtract(moneyToTransfer.amount()), b.currency()));
    }

    private void deposit(Money moneyToTransfer) {
        this.balance.updateAndGet(b -> new Money(b.amount().add(moneyToTransfer.amount()), b.currency()));
    }

    public String id() {
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
               Objects.equals(this.balance.get(), that.balance.get());
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
