package org.example.bank.domain;

import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.AccountId;
import org.example.bank.domain.account.repository.AccountRepository;
import org.example.bank.domain.exceptions.AccountNotFoundException;
import org.example.bank.domain.money.Money;
import org.example.bank.infraestructure.account.repository.inmemory.InMemoryAccountRepositoryConcurrentHashMap;

public class Bank {

    private final AccountRepository accountRepository;

    public Bank() {
        this.accountRepository = new InMemoryAccountRepositoryConcurrentHashMap();
    }

    public Bank(AccountRepository repository) {
        this.accountRepository = repository;
    }

    public void registerAccount(Account anAccount) {
        accountRepository.registerAccount(anAccount);
    }

    public Money getBalance(AccountId id) {
        return accountRepository.getBalance(id);
    }

    public void transfer(Money amount, AccountId origin, AccountId destiny) {
        var originAccount = accountRepository.getAccount(origin);
        var destinyAccount = accountRepository.getAccount(destiny);
        if (originAccount.isEmpty() || destinyAccount.isEmpty()) {
            throw new AccountNotFoundException();
        }
        accountRepository.transfer(amount, origin, destiny);
    }

    @Override
    public String toString() {
        return "Bank{" +
               "accountRepository=" + accountRepository +
               '}';
    }
}
