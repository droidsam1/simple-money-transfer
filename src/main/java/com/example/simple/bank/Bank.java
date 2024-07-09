package com.example.simple.bank;

import com.example.simple.bank.distributed.GlobalLockManager;
import com.example.simple.bank.distributed.LockManager;
import com.example.simple.bank.exceptions.AccountNotFoundException;
import com.example.simple.bank.repository.AccountRepository;
import com.example.simple.bank.repository.GlobalLockManagerAccountRepository;
import java.util.concurrent.locks.ReentrantLock;

public class Bank {

    private final AccountRepository accountRepository;
    private final ReentrantLock lock = new ReentrantLock();
    private final LockManager lockManager;

    public Bank() {
        accountRepository = new GlobalLockManagerAccountRepository();
        lockManager = GlobalLockManager.getInstance();
    }

    public void register(Account newAccount) {
        accountRepository.register(newAccount);
    }

    public Money getBalanceFor(AccountId id) {
        return this.accountRepository.get(id)
                                     .map(Account::balance)
                                     .orElseThrow(AccountNotFoundException::new);
    }

    public void transfer(AccountId from, AccountId to, Money amount) {
        var fromAccount = this.accountRepository.get(from);
        var toAccount = this.accountRepository.get(to);

        if (fromAccount.isEmpty() || toAccount.isEmpty()) {
            throw new AccountNotFoundException();
        }

        pessimisticInternalLocks(from, to, amount, fromAccount.orElseThrow(), toAccount.orElseThrow());
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
        lockManager.performAtomic(from, to, () -> {
            fromAccount.withdraw(amount);
            toAccount.deposit(amount);
        });

    }
}
