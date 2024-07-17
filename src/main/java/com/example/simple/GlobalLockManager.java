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

    public Lock getLock(AccountId accountId) {
        lockMap.putIfAbsent(accountId, new ReentrantLock());
        return lockMap.get(accountId);
    }


    private static class GlobalLockManagerHelper {

        private static final GlobalLockManager instance = new GlobalLockManager();
    }
}
