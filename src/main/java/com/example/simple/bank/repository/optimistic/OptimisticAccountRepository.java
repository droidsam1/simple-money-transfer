package com.example.simple.bank.repository.optimistic;

import com.example.simple.bank.Account;
import com.example.simple.bank.AccountId;
import com.example.simple.bank.Money;
import com.example.simple.bank.exceptions.AccountAlreadyRegisteredException;
import com.example.simple.bank.exceptions.AccountNotFoundException;
import com.example.simple.bank.repository.AccountRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

public class OptimisticAccountRepository implements AccountRepository {

    private final Map<AccountId, Account> accountRepository;

    public OptimisticAccountRepository() {
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

    @Override
    public void transfer(AccountId from, AccountId to, Money amount) {
        var fromAccount = this.get(from).orElseThrow(AccountNotFoundException::new);
        var toAccount = this.get(to).orElseThrow(AccountNotFoundException::new);

        boolean withdrawal = false;
        try {
            withdrawal = retry(() -> fromAccount.compareAndWithdraw(fromAccount.balance(), amount));
            retry(() -> toAccount.compareAndDeposit(toAccount.balance(), amount));
        } catch (Exception exception) {
            //rollback: naive implementation
            if (withdrawal) {
                fromAccount.deposit(amount);
            }
            throw exception;
        }
    }

    //Just a simple retry loop. Note: this is not a good implementation, is infinite and can cause a deadlock
    private boolean retry(BooleanSupplier operation) {
        while (!operation.getAsBoolean()) {
            //do nothing, just retry
        }
        return true;
    }
}
