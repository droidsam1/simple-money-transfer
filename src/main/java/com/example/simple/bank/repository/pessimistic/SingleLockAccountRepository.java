package com.example.simple.bank.repository.pessimistic;

import com.example.simple.bank.Account;
import com.example.simple.bank.AccountId;
import com.example.simple.bank.Money;
import com.example.simple.bank.exceptions.AccountAlreadyRegisteredException;
import com.example.simple.bank.exceptions.AccountNotFoundException;
import com.example.simple.bank.repository.AccountRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SingleLockAccountRepository implements AccountRepository {

    private final Map<AccountId, Account> accountRepository;
    private final Lock lock;

    public SingleLockAccountRepository() {
        this.accountRepository = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
    }

    @Override public void register(Account account) {
        this.accountRepository.compute(account.id(), (id, acc) -> {
            if (acc != null) {
                throw new AccountAlreadyRegisteredException();
            }
            return account;
        });
    }

    @Override public Optional<Account> get(AccountId accountId) {
        return Optional.ofNullable(this.accountRepository.get(accountId));
    }

    @Override public void transfer(AccountId from, AccountId to, Money amount) {
        var fromAccount = this.get(from).orElseThrow(AccountNotFoundException::new);
        var toAccount = this.get(to).orElseThrow(AccountNotFoundException::new);

        try {
            lock.lock();
            fromAccount.withdraw(amount);
            toAccount.deposit(amount);
        } finally {
            lock.unlock();
        }
    }
}
