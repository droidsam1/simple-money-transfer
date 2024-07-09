package com.example.simple.bank.distributed;

import com.example.simple.bank.AccountId;

public interface LockManager {

    void registerLockFor(AccountId accountId);

    void performAtomic(AccountId from, AccountId to, Runnable operation);
}
