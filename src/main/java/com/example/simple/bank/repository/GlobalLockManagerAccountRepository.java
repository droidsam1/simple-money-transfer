package com.example.simple.bank.repository;

import com.example.simple.bank.Account;
import com.example.simple.bank.AccountId;
import com.example.simple.bank.distributed.GlobalLockManager;
import com.example.simple.bank.distributed.LockManager;
import com.example.simple.bank.exceptions.AccountAlreadyRegisteredException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalLockManagerAccountRepository implements AccountRepository {

    private final Map<AccountId, Account> accountRepository;
    private final LockManager lockManager;

    public GlobalLockManagerAccountRepository() {
        accountRepository = new ConcurrentHashMap<>();
        this.lockManager = GlobalLockManager.getInstance();
    }

    @Override public void register(Account account) {
        this.accountRepository.compute(account.id(), (id, acc) -> {
            if (acc != null) {
                throw new AccountAlreadyRegisteredException();
            }
            lockManager.registerLockFor(id);
            return account;
        });
    }

    @Override public Optional<Account> get(AccountId accountId) {
        return Optional.ofNullable(this.accountRepository.get(accountId));
    }
}
