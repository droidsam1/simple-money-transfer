package com.example.simple.bank.distributed;

import com.example.simple.bank.AccountId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    @Override public Optional<Lock> getLockFor(AccountId accountId) {
        return Optional.ofNullable(accountLocks.get(accountId));
    }

    private static class GlobalLockManagerInnerClass {

        private GlobalLockManagerInnerClass() {
        }

        private static final GlobalLockManager INSTANCE = new GlobalLockManager();

    }
}
