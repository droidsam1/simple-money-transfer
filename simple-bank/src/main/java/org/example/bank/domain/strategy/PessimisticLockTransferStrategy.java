package org.example.bank.domain.strategy;

import java.util.Map;
import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.AccountId;
import org.example.bank.domain.money.Money;
import org.example.bank.domain.exceptions.AccountNotFoundException;

public class PessimisticLockTransferStrategy implements TransferStrategy {

    @Override
    public void transfer(Map<AccountId, Account> accounts, Money amount, AccountId origin, AccountId destiny) {
        var originAccount = accounts.get(origin);
        var destinyAccount = accounts.get(destiny);
        if (originAccount == null || destinyAccount == null) {
            throw new AccountNotFoundException();
        }
        if (originAccount.id().hashCode() > destinyAccount.id().hashCode()) {
            synchronized (originAccount) {
                synchronized (destinyAccount) {
                    accounts.computeIfPresent(originAccount.id(), (accountId, account) -> account.withdraw(amount));
                    accounts.computeIfPresent(destinyAccount.id(), (accountId, account) -> account.deposit(amount));
                }
            }
        } else {
            synchronized (destinyAccount) {
                synchronized (originAccount) {
                    accounts.computeIfPresent(originAccount.id(), (accountId, account) -> account.withdraw(amount));
                    accounts.computeIfPresent(destinyAccount.id(), (accountId, account) -> account.deposit(amount));
                }
            }
        }
    }
}
