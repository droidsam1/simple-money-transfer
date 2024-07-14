package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import java.util.concurrent.atomic.AtomicReference;

public class Account {

    private final AccountId id;
    private final AtomicReference<Money> balance;

    public Account(String id, Money initialBalance) {
        this.id = new AccountId(id);
        this.balance = new AtomicReference<>(initialBalance);
    }

    public Money balance() {
        return balance.get();
    }

    public void transfer(Account recipient, Money transferFunds) {
        optimisticTransferApproach(recipient, transferFunds);
    }

    private void optimisticTransferApproach(Account recipient, Money transferFunds) {
        while (true) {
            Money expectedSenderBalance = balance.get();
            Money expectedRecipientBalance = recipient.balance();
            if (this.balance.compareAndSet(expectedSenderBalance, expectedSenderBalance.subtract(transferFunds))) {
                if (recipient.balance.compareAndSet(
                        expectedRecipientBalance,
                        expectedRecipientBalance.add(transferFunds)
                )) {
                    break;
                } else {
                    //rollback: naive implementation
                    this.deposit(transferFunds);
                }
            }
        }
    }

    private void pessimisticTransferApproach(Account recipient, Money transferFunds) {
        if (this.id.compareTo(recipient.id) > 0) {
            synchronized (this) {
                synchronized (recipient) {
                    this.withdraw(transferFunds);
                    recipient.deposit(transferFunds);
                }
            }

        } else {
            synchronized (recipient) {
                synchronized (this) {
                    this.withdraw(transferFunds);
                    recipient.deposit(transferFunds);
                }
            }
        }
    }

    private void withdraw(Money transferFunds) {
        validateEnoughFunds(transferFunds);
        this.balance.updateAndGet(b -> b.subtract(transferFunds));
    }

    private void deposit(Money transferFunds) {
        this.balance.updateAndGet(b -> b.add(transferFunds));
    }

    private void validateEnoughFunds(Money transferFunds) {
        if (balance.get().amount().compareTo(transferFunds.amount()) < 0) {
            throw new InsufficientFundsException();
        }
    }
}
