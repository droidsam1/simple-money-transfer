package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import com.example.simple.exceptions.MisMatchCurrency;
import com.example.simple.exceptions.NegativeTransferAmountException;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public class Account {

    private final String id;
    private final AtomicReference<Money> balance;

    public Account(String id, Money balance) {
        this.id = id;
        this.balance = new AtomicReference<>(balance);
    }

    public Money balance() {
        return balance.get();
    }

    public String id() {
        return id;
    }

    public void transferTo(Account beneficiary, Money transferAmount) {
        optimisticApproach(beneficiary, transferAmount);
        //        pessimisticApproach(beneficiary, transferAmount);
    }

    private void optimisticApproach(Account beneficiary, Money transferAmount) {
        validateCurrency(transferAmount, this.balance());
        validateTransferFundsArePositive(transferAmount);
        validateEnoughFunds(transferAmount, this.balance());
        int maxAttempts = 10_000;
        int attempt = 0;
        while (attempt < maxAttempts) {
            var senderBalance = this.balance.get();
            var beneficiaryBalance = beneficiary.balance.get();

            if (this.balance.compareAndSet(senderBalance, senderBalance.subtract(transferAmount))) {
                if (beneficiary.balance.compareAndSet(beneficiaryBalance, beneficiaryBalance.add(transferAmount))) {
                    return;
                } else {
                    //rollback: naive implementation
                    this.deposit(transferAmount);
                }
            }
            attempt++;
        }
        throw new IllegalStateException("Transfer fails");
    }

    private static void validateTransferFundsArePositive(Money transferAmount) {
        if (transferAmount.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeTransferAmountException();
        }
    }

    private void pessimisticApproach(Account beneficiary, Money transferAmount) {
        validateTransferFundsArePositive(transferAmount);
        if (this.id().compareToIgnoreCase(beneficiary.id()) > 0) {
            synchronized (this) {
                synchronized (beneficiary) {
                    this.withdrawn(transferAmount);
                    beneficiary.deposit(transferAmount);
                }
            }
        } else {
            synchronized (beneficiary) {
                synchronized (this) {
                    this.withdrawn(transferAmount);
                    beneficiary.deposit(transferAmount);
                }
            }
        }
    }

    private void deposit(Money fundsToDeposit) {
        this.balance.updateAndGet(b -> b.add(fundsToDeposit));
    }

    private void withdrawn(Money fundsToWithdraw) {
        this.balance.updateAndGet(b -> {
            validateEnoughFunds(fundsToWithdraw, b);
            return b.subtract(fundsToWithdraw);
        });
    }

    private static void validateEnoughFunds(Money fundsToWithdraw, Money b) {
        if (b.amount().compareTo(fundsToWithdraw.amount()) < 0) {
            throw new InsufficientFundsException();
        }
    }

    private static void validateCurrency(Money fundsToWithdraw, Money b) {
        if (!b.currency().equals(fundsToWithdraw.currency())) {
            throw new MisMatchCurrency();
        }
    }
}
