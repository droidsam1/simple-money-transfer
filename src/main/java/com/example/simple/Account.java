package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import java.util.concurrent.atomic.AtomicReference;

public class Account {

    private final AccountId id;
    private final AtomicReference<Money> balance;

    public Account(String id, Money balance) {
        this(new AccountId(id), balance);
    }

    public Account(AccountId id, Money balance) {
        this.id = id;
        this.balance = new AtomicReference<>(balance);
    }

    public Money balance() {
        return this.balance.get();
    }

    public void withdraw(Money funds) {
        validateEnoughFunds(funds);
        this.balance.updateAndGet(b -> b.subtract(funds));
    }

    private void validateEnoughFunds(Money funds) {
        if (this.balance.get().amount().compareTo(funds.amount()) < 0) {
            throw new InsufficientFundsException();
        }
    }

    public void deposit(Money funds) {
        this.balance.updateAndGet(b -> b.add(funds));
    }

    public void transfer(Account beneficiary, Money funds) {
        optimisticTransfer(beneficiary, funds);
    }

    private void optimisticTransfer(Account beneficiary, Money funds) {

        while (true) {
            Money senderBalance = this.balance.get();
            Money receiverBalance = beneficiary.balance();

            if (this.balance.compareAndSet(senderBalance, senderBalance.subtract(funds))) {
                if (beneficiary.balance.compareAndSet(receiverBalance, receiverBalance.add(funds))) {
                    return;
                } else {
                    //rollback
                    this.deposit(funds);
                }
            }
        }

    }

    private void pessimisticTransfer(Account beneficiary, Money funds) {
        if (this.id.compareTo(beneficiary.id) > 0) {
            synchronized (this) {
                synchronized (beneficiary) {
                    this.withdraw(funds);
                    beneficiary.deposit(funds);
                }
            }
        } else {
            synchronized (beneficiary) {
                synchronized (this) {
                    this.withdraw(funds);
                    beneficiary.deposit(funds);
                }
            }
        }
    }
}
