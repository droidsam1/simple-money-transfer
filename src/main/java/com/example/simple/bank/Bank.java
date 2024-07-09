package com.example.simple.bank;

import com.example.simple.bank.exceptions.AccountNotFoundException;
import com.example.simple.bank.repository.AccountRepository;

public class Bank {

    private final AccountRepository accountRepository;

    public Bank(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
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
        this.accountRepository.transfer(from, to, amount);
    }
}
