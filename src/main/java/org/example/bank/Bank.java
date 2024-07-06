package org.example.bank;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.example.bank.exceptions.AccountAlreadyRegisteredException;
import org.example.bank.exceptions.InvalidTransferAmountException;
import org.example.bank.exceptions.NegativeBalanceAccountRegistrationException;
import org.example.bank.exceptions.UnknownAccountException;

public class Bank {

    private final Map<AccountId, Account> accounts;
    private final ReentrantLock lock = new ReentrantLock();

    //Testing purposes only
    private final Runnable inBetweenTransferBehaviour;

    public Bank() {
        this(() -> {
        });
    }

    public Bank(Runnable inBetweenTransferBehaviour) {
        this.accounts = new ConcurrentHashMap<>();
        this.inBetweenTransferBehaviour = inBetweenTransferBehaviour;
    }

    public void register(Account newClient) {
        if (newClient.balance().amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeBalanceAccountRegistrationException();
        }
        this.accounts.computeIfPresent(newClient.id(), (id, account) -> {
            throw new AccountAlreadyRegisteredException();
        });
        this.accounts.putIfAbsent(newClient.id(), newClient);
    }

    public Money getBalance(AccountId client) {
        return Optional.ofNullable(this.accounts.get(client))
                       .map(Account::balance)
                       .orElseThrow(UnknownAccountException::new);
    }

    public void transfer(AccountId from, AccountId to, Money amount) {
        if (amount.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTransferAmountException();
        }

        Account fromAccount = this.accounts.get(from);
        Account toAccount = this.accounts.get(to);
        if (fromAccount == null || toAccount == null) {
            throw new UnknownAccountException();
        }
        var withdrawHasSuccess = false;
        var depositHasSuccess = false;

        transferOptimistic(amount, withdrawHasSuccess, fromAccount, depositHasSuccess, toAccount);
    }

    private void transferPessimistic(
            Money amount, boolean withdrawHasSuccess, Account fromAccount, boolean depositHasSuccess, Account toAccount
    ) {

        lock.lock();
        try {
            inBetweenTransferBehaviour.run();
            withdrawHasSuccess = fromAccount.withdraw(amount);
            inBetweenTransferBehaviour.run();
            depositHasSuccess = toAccount.deposit(amount);
            inBetweenTransferBehaviour.run();
        } catch (Exception e) {
            // log exception
            rollback(amount, withdrawHasSuccess, fromAccount, depositHasSuccess, toAccount);
        } finally {
            lock.unlock();
        }
    }

    private void transferOptimistic(
            Money amount,
            boolean withdrawHasSuccess,
            Account fromAccount,
            boolean depositHasSuccess,
            Account toAccount
    ) {
        try {
            inBetweenTransferBehaviour.run();

            while (!(withdrawHasSuccess = fromAccount.compareAndWithdraw(fromAccount.balance(), amount))) {

            }
            inBetweenTransferBehaviour.run();
            while (!(depositHasSuccess = toAccount.compareAndDeposit(toAccount.balance(), amount))) {

            }
            inBetweenTransferBehaviour.run();
        } catch (Exception e) {
            // log exception
            rollback(amount, withdrawHasSuccess, fromAccount, depositHasSuccess, toAccount);
        }
    }

    private static void rollback(
            Money amount,
            boolean withdrawHasSuccess,
            Account fromAccount,
            boolean depositHasSuccess,
            Account toAccount
    ) {

        //rollback
        if (withdrawHasSuccess) {
            fromAccount.deposit(amount);
        }
        if (depositHasSuccess) {
            toAccount.withdraw(amount);
        }
    }
}
