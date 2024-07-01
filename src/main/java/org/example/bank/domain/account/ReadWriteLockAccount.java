package org.example.bank.domain.account;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.example.bank.domain.exceptions.InsufficientFundsException;
import org.example.bank.domain.exceptions.NegativeTransferAmountException;
import org.example.bank.domain.money.Money;

public final class ReadWriteLockAccount implements Account {

    private final AccountId id;
    private Money balance;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ReadWriteLockAccount(AccountId id, Money balance) {
        this.id = id;
        this.balance = balance;
    }

    public ReadWriteLockAccount(String id, Money balance) {
        this(new AccountId(id), balance);
    }

    @Override
    public ReadWriteLockAccount withdraw(Money amount) {
        lock.writeLock().lock();
        try {
            validateEnoughFunds(amount);
            validateNonNegativeWithdraw(amount);
            this.balance = this.balance.subtract(amount);
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void validateNonNegativeWithdraw(Money amount) {
        if (amount.isNegative()) {
            throw new NegativeTransferAmountException();
        }
    }

    private void validateEnoughFunds(Money amount) {
        if (balance.amount().compareTo(amount.amount()) < 0) {
            throw new InsufficientFundsException();
        }
    }

    @Override
    public ReadWriteLockAccount deposit(Money amount) {
        lock.writeLock().lock();
        try {
            this.balance = this.balance.add(amount);
        } finally {
            lock.writeLock().unlock();
        }
        return this;
    }

    @Override
    public AccountId id() {
        return id;
    }

    @Override
    public  boolean compareAndSubtract(Money originBalance, Money moneyToSubtract) {
        if (!balance().equals(originBalance)) {
            return false;
        }
        withdraw(moneyToSubtract);
        return true;
    }

    @Override
    public  boolean compareAndAdd(Money originBalance, Money moneyToAdd) {
        if (!balance().equals(originBalance)) {
            return false;
        }
        deposit(moneyToAdd);
        return true;
    }

    public synchronized Money balance() {
        lock.readLock().lock();
        try {
            return balance;
        } finally {
            lock.readLock().unlock();
        }
    }
}
