package com.example.simple;

import com.example.simple.Account.AccountId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GlobalLockManager {

    private final Map<AccountId, Lock> lockMap = new ConcurrentHashMap<>();

    private GlobalLockManager() {

    }

    public static GlobalLockManager getInstance() {
        return GlobalLockManagerInner.INSTANCE;
    }

    public void transfer(Account from, Account to, Money funds) {
        lockMap.putIfAbsent(from.id(), new ReentrantLock());
        lockMap.putIfAbsent(to.id(), new ReentrantLock());

        if (from.id().compareTo(to.id()) > 0) {
            lockMap.get(from.id()).lock();
            lockMap.get(to.id()).lock();
        } else {
            lockMap.get(to.id()).lock();
            lockMap.get(from.id()).lock();
        }

        try {
            from.withdraw(funds);
            to.deposit(funds);
        } finally {
            lockMap.get(from.id()).unlock();
            lockMap.get(to.id()).unlock();
        }
    }

    private static class GlobalLockManagerInner {

        private static final GlobalLockManager INSTANCE = new GlobalLockManager();
    }

}
