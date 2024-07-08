package com.example.simple.bank.distributed;

import com.example.simple.bank.AccountId;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

public interface LockManager {

    void registerLockFor(AccountId accountId);

    Optional<Lock> getLockFor(AccountId accountId);
}
