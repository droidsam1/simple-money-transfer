package org.example.bank.domain.strategy;

import java.util.Map;
import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.AccountId;
import org.example.bank.domain.money.Money;

public class OptimisticLockTransferStrategy implements TransferStrategy {

    @Override
    public void transfer(Map<AccountId, Account> accounts, Money amount, AccountId origin, AccountId destiny) {
        var originAccount = accounts.get(origin);
        var destinyAccount = accounts.get(destiny);
        retry(() -> {
                var originBalance = originAccount.balance();
                originAccount.compareAndSetBalance(originBalance, originBalance.subtract(amount));
        });
        retry(() -> {
                var originBalance = destinyAccount.balance();
                destinyAccount.compareAndSetBalance(originBalance, originBalance.add(amount));
        });
    }

    //Just a simple retry loop
    private void retry(Runnable operation) {
        while (true) {
            try {
                operation.run();
                break;
            } catch (Exception e) {
                // The loop will continue because success is still false
            }
        }
    }
}
