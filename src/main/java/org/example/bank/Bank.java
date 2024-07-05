package org.example.bank;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.example.bank.exceptions.AccountAlreadyRegisteredException;
import org.example.bank.exceptions.InvalidTransferAmountException;
import org.example.bank.exceptions.NegativeBalanceAccountRegistrationException;
import org.example.bank.exceptions.UnknownAccountException;

public class Bank {

    private final Map<AccountId, Account> accounts;
    private final ReentrantLock lock = new ReentrantLock();

    public Bank() {
        this.accounts = new ConcurrentHashMap<>();
    }

    public void register(Account newClient) {
        if (newClient.balance().amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeBalanceAccountRegistrationException();
        }
        this.accounts.computeIfPresent(newClient.id(), (id, account) -> {
            throw new AccountAlreadyRegisteredException();
        });
        this.accounts.putIfAbsent(newClient.id(), newClient);
    }

    public Money getBalance(AccountId client) {
        return Optional.ofNullable(this.accounts.get(client))
                       .map(Account::balance)
                       .orElseThrow(UnknownAccountException::new);
    }

    public void transfer(AccountId from, AccountId to, Money amount) {
        if (amount.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTransferAmountException();
        }

        Account fromAccount = this.accounts.get(from);
        Account toAccount = this.accounts.get(to);
        if (fromAccount == null || toAccount == null) {
            throw new UnknownAccountException();
        }

        lock.lock();
        try {
            fromAccount.withdraw(amount);
            toAccount.deposit(amount);
        } finally {
            lock.unlock();
        }
    }
}
