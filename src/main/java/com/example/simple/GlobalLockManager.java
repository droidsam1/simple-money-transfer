package com.example.simple;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GlobalLockManager {

    private GlobalLockManager() {
    }

    public static GlobalLockManager getInstance() {
        return InnerGlobalLockManager.instance;
    }

    private final Map<AccountId, Lock> lockMap = new ConcurrentHashMap<>();

    public void transfer(Account sender, Account beneficiary, Money fundsToTransfer) {
        lockMap.putIfAbsent(sender.id(), new ReentrantLock());
        lockMap.putIfAbsent(beneficiary.id(), new ReentrantLock());

        try {
            //atomic operation
            if (sender.id().value().compareToIgnoreCase(beneficiary.id().value()) > 0) {
                lockMap.get(sender.id()).lock();
                lockMap.get(beneficiary.id()).lock();
            } else {
                lockMap.get(beneficiary.id()).lock();
                lockMap.get(sender.id()).lock();
            }
            sender.withdraw(fundsToTransfer);
            beneficiary.deposit(fundsToTransfer);
        } finally {
            lockMap.get(sender.id()).unlock();
            lockMap.get(beneficiary.id()).unlock();
        }
    }

    private static class InnerGlobalLockManager {

        private static final GlobalLockManager instance = new GlobalLockManager();
    }
}
