package org.example.bank.infraestructure.account.repository.inmemory.strategy;

import java.util.Map;
import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.AccountId;
import org.example.bank.domain.money.Money;

public interface TransferStrategy {

    void transfer(Map<AccountId, Account> accounts, Money amount, AccountId origin, AccountId destiny);
}
