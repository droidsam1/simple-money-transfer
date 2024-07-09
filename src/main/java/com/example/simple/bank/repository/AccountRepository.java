package com.example.simple.bank.repository;

import com.example.simple.bank.Account;
import com.example.simple.bank.AccountId;
import java.util.Optional;

public interface AccountRepository {

    void register(Account account);

    Optional<Account> get(AccountId accountId);
}
