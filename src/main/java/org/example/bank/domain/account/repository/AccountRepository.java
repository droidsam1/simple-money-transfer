package org.example.bank.domain.account.repository;

import java.util.Optional;
import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.AccountId;
import org.example.bank.domain.money.Money;

public interface AccountRepository {

    void registerAccount(Account anAccount);

    Money getBalance(AccountId id);

    void transfer(Money amount, AccountId origin, AccountId destiny);

    Optional<Account> getAccount(AccountId id);
}
