package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import com.example.simple.exceptions.MismatchCurrencyException;
import com.example.simple.exceptions.NegativeTransferAmountException;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public class Account {

    private final AccountId id;
    private final AtomicReference<Money> balance;
    private final GlobalLockManager lockManager = GlobalLockManager.getInstance();

    public Account(AccountId id, Money balance) {
        this.id = id;
        this.balance = new AtomicReference<>(balance);
    }

    public Account(String id, Money balance) {
        this(new AccountId(id), balance);
    }

    public Money balance() {
        return balance.get();
    }

    public AccountId id() {
        return id;
    }

    public void transfer(Account beneficiary, Money transferFunds) {
        if (beneficiary == this) {
            return;
        }
        validateCurrency(transferFunds);
        validatePositiveAmount(transferFunds);
        validateSenderHasEnoughFunds(transferFunds);

//                optimistic(beneficiary, transferFunds);
//                        pessimisticTransfer(beneficiary, transferFunds);
        pessimisticGlobalLock(beneficiary, transferFunds);
    }

    private void pessimisticGlobalLock(Account beneficiary, Money transferFunds) {
        this.lockManager.transfer(this, beneficiary, transferFunds);
    }

    private void optimistic(Account beneficiary, Money transferFunds) {
        while (true) {
            Money expectedBalanceSender = this.balance.get();
            Money expectedBalanceReceiver = beneficiary.balance.get();

            if (this.balance.compareAndSet(expectedBalanceSender, this.balance.get().subtract(transferFunds))) {
                if (beneficiary.balance.compareAndSet(
                        expectedBalanceReceiver,
                        beneficiary.balance.get().add(transferFunds)
                )) {
                    return;
                } else {
                    //rollback
                    this.deposit(transferFunds);
                }
            }
        }
    }

    private void pessimisticTransfer(Account beneficiary, Money transferFunds) {
        if (this.id().compareTo(beneficiary.id) > 0) {
            synchronized (this) {
                synchronized (beneficiary) {
                    this.withdraw(transferFunds);
                    beneficiary.deposit(transferFunds);
                }
            }
        } else {
            synchronized (beneficiary) {
                synchronized (this) {
                    this.withdraw(transferFunds);
                    beneficiary.deposit(transferFunds);
                }
            }
        }
    }

    public void deposit(Money transferFunds) {
        this.balance.updateAndGet(b -> b.add(transferFunds));
    }

    public void withdraw(Money transferFunds) {
        this.balance.updateAndGet(b -> {
            validateSenderHasEnoughFunds(transferFunds);
            return b.subtract(transferFunds);
        });
    }

    private void validateSenderHasEnoughFunds(Money transferFunds) {
        if (this.balance().amount().compareTo(transferFunds.amount()) < 0) {
            throw new InsufficientFundsException();
        }
    }

    private void validatePositiveAmount(Money transferFunds) {
        if (transferFunds.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeTransferAmountException();
        }
    }

    private void validateCurrency(Money transferFunds) {
        if (!this.balance().currency().equals(transferFunds.currency())) {
            throw new MismatchCurrencyException();
        }
    }

    public record AccountId(String value) implements Comparable<AccountId> {

        @Override public int compareTo(AccountId o) {
            return this.value.compareToIgnoreCase(o.value);
        }
    }
}
