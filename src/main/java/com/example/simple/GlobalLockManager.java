package com.example.simple;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GlobalLockManager {

    private final ConcurrentHashMap<AccountId, Lock> lockMap = new ConcurrentHashMap<>();

    private GlobalLockManager() {
    }

    public static GlobalLockManager getInstance() {
        return GlobalLockManagerHelper.instance;
    }

    public void atomic(AccountId one, AccountId another, Runnable function) {
        lockMap.putIfAbsent(one, new ReentrantLock());
        lockMap.putIfAbsent(another, new ReentrantLock());
        if (one.compareTo(another) > 0) {
            lockMap.get(one).lock();
            lockMap.get(another).lock();
        } else {
            lockMap.get(another).lock();
            lockMap.get(one).lock();
        }
        try {
            function.run();
        } finally {
            lockMap.get(another).unlock();
            lockMap.get(one).unlock();
        }
    }

    public Lock getLock(AccountId accountId) {
        lockMap.putIfAbsent(accountId, new ReentrantLock());
        return lockMap.get(accountId);
    }


    private static class GlobalLockManagerHelper {

        private static final GlobalLockManager instance = new GlobalLockManager();
    }
}
