package org.example.bank.infraestructure.account.repository.inmemory.strategy;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.AccountId;
import org.example.bank.domain.exceptions.AccountNotFoundException;
import org.example.bank.domain.money.Money;

/**
 * This provides a more robust pessimistic lock strategy to transfer money between accounts with a consistent order of
 * locks not depending on external factors
 */
public class RevisitedPessimisticLockTransferStrategy implements TransferStrategy {

    private final Map<AccountId, Lock> locks;

    public RevisitedPessimisticLockTransferStrategy() {
        locks = new ConcurrentHashMap<>();
    }

    @Override
    public void transfer(Map<AccountId, Account> accounts, Money amount, AccountId origin, AccountId destiny) {
        var originAccount = accounts.get(origin);
        var destinyAccount = accounts.get(destiny);
        if (originAccount == null || destinyAccount == null) {
            throw new AccountNotFoundException();
        }

        locks.putIfAbsent(origin, new ReentrantLock());
        locks.putIfAbsent(destiny, new ReentrantLock());

        var sortedLocks = Stream.of(origin, destiny)
                                .sorted(Comparator.comparingInt(System::identityHashCode))
                                .map(locks::get)
                                .toList();
        try {
            sortedLocks.getFirst().lock();
            sortedLocks.getLast().lock();
            accounts.computeIfPresent(originAccount.id(), (accountId, account) -> account.withdraw(amount));
            accounts.computeIfPresent(destinyAccount.id(), (accountId, account) -> account.deposit(amount));
        } finally {
            sortedLocks.getLast().unlock();
            sortedLocks.getFirst().unlock();
        }
    }
}
