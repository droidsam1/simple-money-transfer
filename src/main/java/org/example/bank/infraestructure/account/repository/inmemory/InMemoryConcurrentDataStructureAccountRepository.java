package org.example.bank.infraestructure.account.repository.inmemory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.AccountId;
import org.example.bank.domain.account.repository.AccountRepository;
import org.example.bank.domain.exceptions.AccountNotFoundException;
import org.example.bank.domain.money.Money;

public class InMemoryConcurrentDataStructureAccountRepository implements AccountRepository {

    private final Map<AccountId, Account> accounts;

    public InMemoryConcurrentDataStructureAccountRepository() {
        accounts = new ConcurrentHashMap<>();
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
        var originAccount = accounts.get(origin);
        var destinyAccount = accounts.get(destiny);
        if (originAccount == null || destinyAccount == null) {
            throw new AccountNotFoundException();
        }
        accounts.computeIfPresent(originAccount.id(), (accountId, account) -> account.withdraw(amount));
        accounts.computeIfPresent(destinyAccount.id(), (accountId, account) -> account.deposit(amount));
    }

    @Override public Optional<Account> getAccount(AccountId id) {
        return Optional.ofNullable(accounts.get(id));
    }
}
