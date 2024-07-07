package org.example.bank.infraestructure.account.repository.inmemory.strategy;

import java.util.Map;
import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.AccountId;
import org.example.bank.domain.money.Money;

/**
 * Pessimistic lock transfer strategy: This can lead to deadlocks, but it is a simple way to avoid them. See
 * {@link RevisitedPessimisticLockTransferStrategy} for a more robust solution.
 */
public class PessimisticLockTransferStrategy implements TransferStrategy {

    @Override
    public void transfer(Map<AccountId, Account> accounts, Money amount, AccountId origin, AccountId destiny) {
        var originAccount = accounts.get(origin);
        var destinyAccount = accounts.get(destiny);
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
