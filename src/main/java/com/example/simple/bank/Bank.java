package com.example.simple.bank;

import com.example.simple.bank.exceptions.AccountAlreadyRegisteredException;
import com.example.simple.bank.exceptions.AccountNotFoundException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Bank {

    private final Map<AccountId, Account> accountRepository;

    public Bank() {
        accountRepository = new ConcurrentHashMap<>();
    }

    public void register(Account newAccount) {
        this.accountRepository.compute(newAccount.id(), (id, account) -> {
            if (account != null) {
                throw new AccountAlreadyRegisteredException();
            }
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
        fromAccount.withdraw(amount);
        toAccount.deposit(amount);
    }
}
