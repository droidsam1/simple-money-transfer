package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import java.util.concurrent.atomic.AtomicReference;

public class Account {

    private final AccountId id;
    private final AtomicReference<Money> balance;

    public Account(String id, Money balance) {
        this.balance = new AtomicReference<>(balance);
        this.id = new AccountId(id);
    }

    public Money balance() {
        return balance.get();
    }

    public void transfer(Account recipient, Money funds) {
        while (true) {
            Money expectedValueSender = this.balance.get();
            Money expectedValueRecipient = recipient.balance.get();
            if (this.balance.compareAndSet(expectedValueSender, expectedValueSender.subtract(funds))) {
                if (recipient.balance.compareAndSet(expectedValueRecipient, expectedValueRecipient.add(funds))) {
                    //success
                    break;
                } else {
                    //rollback withdraw
                    deposit(funds);
                }
            }
        }
    }

    private void deposit(Money funds) {
        this.balance.updateAndGet(b -> b.add(funds));
    }

    private void withdraw(Money funds) {
        this.balance.updateAndGet(b -> {
            validateEnoughFunds(funds);
            return b.subtract(funds);
        });
    }

    private void validateEnoughFunds(Money funds) {
        if (this.balance.get().amount().compareTo(funds.amount()) < 0) {
            throw new InsufficientFundsException();
        }
    }
}
