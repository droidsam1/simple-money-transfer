package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import com.example.simple.exceptions.NegativeAmountTransferException;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public class Account {

    private final AccountId id;
    private final AtomicReference<Money> balance;


    public Account(String id, Money balance) {
        this.id = new AccountId(id);
        this.balance = new AtomicReference<>(balance);
    }

    public Money getBalance() {
        return balance.get();
    }

    public void transfer(Account recipient, Money funds) {
        validateFundsArePositive(funds);

        if (this.id.compareTo(recipient.id) > 0) {
            GlobalLockManager.getInstance().getLock(this.id).lock();
            GlobalLockManager.getInstance().getLock(recipient.id).lock();
        } else {
            GlobalLockManager.getInstance().getLock(recipient.id).lock();
            GlobalLockManager.getInstance().getLock(this.id).lock();
        }
        try {
            this.withdraw(funds);
            recipient.deposit(funds);
        } finally {
            GlobalLockManager.getInstance().getLock(recipient.id).unlock();
            GlobalLockManager.getInstance().getLock(this.id).unlock();
        }
    }

    private void deposit(Money funds) {
        this.balance.updateAndGet(b -> b.add(funds));
    }

    private void withdraw(Money funds) {
        this.balance.updateAndGet(b -> {
            if (b.amount().compareTo(funds.amount()) < 0) {
                throw new InsufficientFundsException();
            }
            return b.subtract(funds);
        });
    }

    private void validateFundsArePositive(Money funds) {
        if (funds.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeAmountTransferException();
        }
    }
}
