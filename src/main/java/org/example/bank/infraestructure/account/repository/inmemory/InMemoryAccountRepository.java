package org.example.bank.infraestructure.account.repository.inmemory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.AccountId;
import org.example.bank.domain.account.repository.AccountRepository;
import org.example.bank.domain.exceptions.AccountNotFoundException;
import org.example.bank.domain.money.Money;
import org.example.bank.infraestructure.account.repository.inmemory.strategy.TransferStrategy;

public class InMemoryAccountRepository implements AccountRepository {

    private final Map<AccountId, Account> accounts;
    private final TransferStrategy transferStrategy;

    public InMemoryAccountRepository(TransferStrategy transferStrategy) {
        this.accounts = new HashMap<>();
        this.transferStrategy = transferStrategy;
    }

    @Override public void registerAccount(Account anAccount) {
        accounts.putIfAbsent(anAccount.id(), anAccount);
    }

    @Override public Money getBalance(AccountId id) {
        var account = accounts.get(id);
        if (account == null) {
            throw new AccountNotFoundException();
        }
        return account.balance();
    }

    @Override public void transfer(Money amount, AccountId origin, AccountId destiny) {
        this.transferStrategy.transfer(accounts, amount, origin, destiny);
    }

    @Override
    public Optional<Account> getAccount(AccountId id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "." + this.transferStrategy.getClass().getSimpleName();
    }
}
