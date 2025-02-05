package org.example.bank.infraestructure.account.repository.inmemory.strategy;

import java.util.Map;
import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.AccountId;
import org.example.bank.domain.money.Money;

public class SerializedTransferStrategy implements TransferStrategy {

    @Override
    public synchronized void transfer(Map<AccountId, Account> accounts, Money amount, AccountId origin, AccountId destiny) {
            accounts.computeIfPresent(origin, (accountId, account) -> account.withdraw(amount));
            accounts.computeIfPresent(destiny, (accountId, account) -> account.deposit(amount));
        }

}
