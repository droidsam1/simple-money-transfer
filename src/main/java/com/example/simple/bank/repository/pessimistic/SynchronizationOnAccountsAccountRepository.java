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

public class SynchronizationOnAccountsAccountRepository implements AccountRepository {

    private final Map<AccountId, Account> accountRepository;

    public SynchronizationOnAccountsAccountRepository() {
        this.accountRepository = new ConcurrentHashMap<>();
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
        Account fromAccount = get(from).orElseThrow(AccountNotFoundException::new);
        Account toAccount = get(to).orElseThrow(AccountNotFoundException::new);

        if (fromAccount.id().value().compareToIgnoreCase(toAccount.id().value()) > 0) {
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
}
