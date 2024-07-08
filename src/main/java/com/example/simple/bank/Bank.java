package com.example.simple.bank;

import com.example.simple.bank.distributed.GlobalLockManager;
import com.example.simple.bank.distributed.LockManager;
import com.example.simple.bank.exceptions.AccountAlreadyRegisteredException;
import com.example.simple.bank.exceptions.AccountNotFoundException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Bank {

    private final Map<AccountId, Account> accountRepository;
    private final ReentrantLock lock = new ReentrantLock();
    private final LockManager lockManager;

    public Bank() {
        accountRepository = new ConcurrentHashMap<>();
        lockManager = GlobalLockManager.getInstance();
    }

    public void register(Account newAccount) {
        this.accountRepository.compute(newAccount.id(), (id, account) -> {
            if (account != null) {
                throw new AccountAlreadyRegisteredException();
            }
            lockManager.registerLockFor(id);
            return newAccount;
        });
    }

    public Money getBalanceFor(AccountId id) {
        return Optional.ofNullable(this.accountRepository.get(id))
                       .map(Account::balance)
                       .orElseThrow(AccountNotFoundException::new);
    }

    public void transfer(AccountId from, AccountId to, Money amount) {
        Account fromAccount = this.accountRepository.get(from);
        Account toAccount = this.accountRepository.get(to);

        if (fromAccount == null || toAccount == null) {
            throw new AccountNotFoundException();
        }

        pessimisticInternalLocks(from, to, amount, fromAccount, toAccount);
        //        pessimisticSynchronized(fromAccount, toAccount, amount);
        //        pessimisticOneReentrantLock(amount, fromAccount, toAccount);
        //        optimistic(amount, fromAccount, toAccount);
    }

    private void pessimisticSynchronized(Account fromAccount, Account toAccount, Money amount) {
        if (fromAccount.id().toString().compareTo(toAccount.id().toString()) < 0) {
            synchronized (fromAccount) {
                synchronized (toAccount) {
                    fromAccount.withdraw(amount);
                    toAccount.deposit(amount);
                }
            }
        } else {
            synchronized (toAccount) {
                synchronized (fromAccount) {
                    fromAccount.withdraw(amount);
                    toAccount.deposit(amount);
                }
            }
        }
    }

    private void optimistic(Money amount, Account fromAccount, Account toAccount) {
        boolean success = false;
        while (!success) {
            fromAccount.withdraw(amount);
            try {
                toAccount.deposit(amount);
                success = true;
            } catch (Exception e) {
                fromAccount.deposit(amount);
            }
        }
    }

    private void pessimisticOneReentrantLock(Money amount, Account fromAccount, Account toAccount) {
        try {
            lock.lock();
            fromAccount.withdraw(amount);
            toAccount.deposit(amount);
        } finally {
            lock.unlock();
        }
    }

    private void pessimisticInternalLocks(
            AccountId from,
            AccountId to,
            Money amount,
            Account fromAccount,
            Account toAccount
    ) {

        Lock fromLock = lockManager.getLockFor(from).orElseThrow();
        Lock toLock = lockManager.getLockFor(to).orElseThrow();

        // Ensure locks are always acquired in a consistent order to prevent deadlocks
        if (fromAccount.id().toString().compareTo(toAccount.id().toString()) < 0) {
            fromLock.lock();
            toLock.lock();
        } else {
            toLock.lock();
            fromLock.lock();
        }

        try {
            fromAccount.withdraw(amount);
            toAccount.deposit(amount);
        } finally {
            fromLock.unlock();
            toLock.unlock();
        }
    }
}
