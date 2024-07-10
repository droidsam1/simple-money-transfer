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
        if (transferAmount.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeTransferAmountException();
        }
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
        this.balance.updateAndGet(b -> {
            if (!b.currency().equals(fundsToDeposit.currency())) {
                throw new MisMatchCurrency();
            }
            return new Money(b.amount().add(fundsToDeposit.amount()), b.currency());
        });
    }

    private void withdrawn(Money fundsToWithdraw) {
        this.balance.updateAndGet(b -> {
            if (b.amount().compareTo(fundsToWithdraw.amount()) < 0) {
                throw new InsufficientFundsException();
            }
            if (!b.currency().equals(fundsToWithdraw.currency())) {
                throw new MisMatchCurrency();
            }
            return new Money(b.amount().subtract(fundsToWithdraw.amount()), b.currency());
        });
    }
}
