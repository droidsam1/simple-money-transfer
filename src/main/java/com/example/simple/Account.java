package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import com.example.simple.exceptions.NegativeTransferAttemptException;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public class Account {

    private final AccountId id;
    private final AtomicReference<Money> balance;

    public Account(String id, Money balance) {
        this.id = new AccountId(id);
        this.balance = new AtomicReference<>(balance);
    }

    public Money balance() {
        return balance.get();
    }

    public void transfer(Account recipient, Money funds) {
        validateFundsArePositive(funds);
        //this should be atomic

        while (true) {
            Money senderExpectedBalance = this.balance.get();
            Money receiverExpectedBalance = recipient.balance();
            validateEnoughFunds(funds, senderExpectedBalance);

            if (this.balance.compareAndSet(senderExpectedBalance, senderExpectedBalance.subtract(funds))) {
                if (recipient.balance.compareAndSet(receiverExpectedBalance, receiverExpectedBalance.add(funds))) {
                    //success
                    break;
                } else {
                    //rollback withdraw
                    this.deposit(funds);
                }
            }
        }
    }

    private void deposit(Money funds) {
        this.balance.updateAndGet(b -> b.add(funds));
    }

    private void withdraw(Money funds) {
        this.balance.updateAndGet(b -> {
            validateEnoughFunds(funds, b);
            return b.subtract(funds);
        });
    }

    private static void validateEnoughFunds(Money funds, Money b) {
        if (b.amount().compareTo(funds.amount()) < 0) {
            throw new InsufficientFundsException();
        }
    }

    private void validateFundsArePositive(Money funds) {
        if (funds.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeTransferAttemptException();
        }
    }
}
