package com.example.simple.bank;

import com.example.simple.bank.exceptions.AccountNotFoundException;
import com.example.simple.bank.repository.AccountRepository;
import com.example.simple.bank.repository.pessimistic.SingleLockAccountRepository;

public class Bank {

    private final AccountRepository accountRepository;

    public Bank() {
        accountRepository = new SingleLockAccountRepository();
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
}
