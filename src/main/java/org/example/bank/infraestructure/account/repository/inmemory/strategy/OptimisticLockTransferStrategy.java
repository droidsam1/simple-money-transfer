package org.example.bank.infraestructure.account.repository.inmemory.strategy;

import java.util.Map;
import java.util.function.BooleanSupplier;
import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.AccountId;
import org.example.bank.domain.money.Money;

public class OptimisticLockTransferStrategy implements TransferStrategy {

    @Override
    public void transfer(Map<AccountId, Account> accounts, Money amount, AccountId origin, AccountId destiny) {
        var originAccount = accounts.get(origin);
        var destinyAccount = accounts.get(destiny);
        retry(() -> originAccount.compareAndSubtract(originAccount.balance(), amount));
        retry(() -> destinyAccount.compareAndAdd(destinyAccount.balance(), amount));
    }

    //Just a simple retry loop
    private void retry(BooleanSupplier operation) {
        while (!operation.getAsBoolean()) {
        }
    }
}
