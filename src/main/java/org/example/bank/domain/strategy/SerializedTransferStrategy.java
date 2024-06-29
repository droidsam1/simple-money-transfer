package org.example.bank.domain.strategy;

import java.util.Map;
import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.AccountId;
import org.example.bank.domain.money.Money;

public class SerializedTransferStrategy implements TransferStrategy {

    @Override
    public void transfer(Map<AccountId, Account> accounts, Money amount, AccountId origin, AccountId destiny) {
        synchronized (this) {
            accounts.computeIfPresent(origin, (accountId, account) -> account.withdraw(amount));
            accounts.computeIfPresent(destiny, (accountId, account) -> account.deposit(amount));
        }
    }

}
