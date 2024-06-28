package org.example.bank.domain;

import java.util.HashMap;
import java.util.Map;
import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.AccountId;
import org.example.bank.domain.exceptions.AccountNotFoundException;
import org.example.bank.domain.money.Money;
import org.example.bank.domain.strategy.SerializedTransferStrategy;
import org.example.bank.domain.strategy.TransferStrategy;

public class Bank {

    private final Map<AccountId, Account> accounts;
    private final TransferStrategy transferStrategy;

    public Bank() {
        accounts = new HashMap<>();
        this.transferStrategy = new SerializedTransferStrategy();
    }

    public Bank(TransferStrategy transferStrategy) {
        accounts = new HashMap<>();
        this.transferStrategy = transferStrategy;
    }

    public void registerAccount(Account anAccount) {
        accounts.putIfAbsent(anAccount.id(), anAccount);
    }

    public Money getBalance(AccountId id) {
        var account = accounts.get(id);
        if (account == null) {
            throw new AccountNotFoundException();
        }
        return account.balance();
    }

    public void transfer(Money amount, AccountId origin, AccountId destiny) {
        this.transferStrategy.transfer(accounts, amount, origin, destiny);
    }
}
