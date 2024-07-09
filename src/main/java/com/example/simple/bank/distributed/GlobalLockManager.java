package com.example.simple.bank.distributed;

import com.example.simple.bank.AccountId;
import com.example.simple.bank.exceptions.AccountNotFoundException;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

public class GlobalLockManager implements LockManager {

    private final Map<AccountId, Lock> accountLocks;

    private GlobalLockManager() {
        accountLocks = new ConcurrentHashMap<>();
    }

    public static GlobalLockManager getInstance() {
        return GlobalLockManager.GlobalLockManagerInnerClass.INSTANCE;
    }

    @Override public void registerLockFor(AccountId accountId) {
        accountLocks.putIfAbsent(accountId, new ReentrantLock());
    }


    @Override public void performAtomic(AccountId from, AccountId to, Runnable operation) {
        Lock fromLock = accountLocks.get(from);
        Lock toLock = accountLocks.get(to);

        if (fromLock == null || toLock == null) {
            throw new AccountNotFoundException();
        }

        var sortedLocks = Stream.of(fromLock, toLock)
                                .sorted(Comparator.comparingInt(System::identityHashCode))
                                .toList();

        sortedLocks.getFirst().lock();
        sortedLocks.getLast().lock();
        try {
            operation.run();
        } finally {
            sortedLocks.getLast().unlock();
            sortedLocks.getFirst().unlock();
        }
    }

    private static class GlobalLockManagerInnerClass {

        private GlobalLockManagerInnerClass() {
        }

        private static final GlobalLockManager INSTANCE = new GlobalLockManager();

    }
}
