package org.example.bank.domain.strategy;

import java.util.Map;
import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.AccountId;
import org.example.bank.domain.money.Money;
import org.example.bank.domain.exceptions.AccountNotFoundException;

public class SerializedTransferStrategy implements TransferStrategy {

    @Override
    public void transfer(Map<AccountId, Account> accounts, Money amount, AccountId origin, AccountId destiny) {
        var originAccount = accounts.get(origin);
        var destinyAccount = accounts.get(destiny);
        if (originAccount == null || destinyAccount == null) {
            throw new AccountNotFoundException();
        }
        accounts.computeIfPresent(originAccount.id(), (accountId, account) -> account.withdraw(amount));
        accounts.computeIfPresent(destinyAccount.id(), (accountId, account) -> account.deposit(amount));
    }

}
