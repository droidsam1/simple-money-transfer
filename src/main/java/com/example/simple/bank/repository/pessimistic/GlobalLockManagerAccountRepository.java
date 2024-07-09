package com.example.simple.bank.repository.pessimistic;

import com.example.simple.bank.Account;
import com.example.simple.bank.AccountId;
import com.example.simple.bank.Money;
import com.example.simple.bank.distributed.GlobalLockManager;
import com.example.simple.bank.distributed.LockManager;
import com.example.simple.bank.exceptions.AccountAlreadyRegisteredException;
import com.example.simple.bank.exceptions.AccountNotFoundException;
import com.example.simple.bank.repository.AccountRepository;
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

    @Override public void transfer(AccountId from, AccountId to, Money amount) {
        var fromAccount = this.get(from).orElseThrow(AccountNotFoundException::new);
        var toAccount = this.get(to).orElseThrow(AccountNotFoundException::new);

        lockManager.performAtomic(from, to, () -> {
            fromAccount.withdraw(amount);
            toAccount.deposit(amount);
        });
    }


}
