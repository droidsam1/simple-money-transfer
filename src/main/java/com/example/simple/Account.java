package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import com.example.simple.exceptions.MisMatchCurrencyException;
import java.math.BigDecimal;
import java.util.Objects;

public class Account {

    private final String id;
    private Money balance;

    public Account(String id, Money balance) {
        this.id = Objects.requireNonNull(id);
        this.balance = Objects.requireNonNull(balance);
    }

    public Money balance() {
        return this.balance;
    }

    public void transfer(Account beneficiary, Money balanceToTransfer) {
        validateCurrency(balanceToTransfer);
        validateBalanceToTransferIsPositive(balanceToTransfer);
        validateEnoughFunds(balanceToTransfer);

        //atomic operation
        if (this.id.compareToIgnoreCase(beneficiary.id) > 0) {
            synchronized (this) {
                synchronized (beneficiary) {
                    this.withdraw(balanceToTransfer);
                    beneficiary.deposit(balanceToTransfer);
                }
            }
        } else {
            synchronized (beneficiary) {
                synchronized (this) {
                    this.withdraw(balanceToTransfer);
                    beneficiary.deposit(balanceToTransfer);
                }
            }
        }
    }

    private void deposit(Money balanceToTransfer) {
        this.balance = this.balance.add(balanceToTransfer);
    }

    private void withdraw(Money balanceToTransfer) {
        this.balance = this.balance.subtract(balanceToTransfer);
    }

    private void validateEnoughFunds(Money balanceToTransfer) {
        if (balance().amount().compareTo(balanceToTransfer.amount()) < 0) {
            throw new InsufficientFundsException();
        }
    }

    private void validateBalanceToTransferIsPositive(Money balanceToTransfer) {
        if (balanceToTransfer.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeTransferException();
        }
    }

    private void validateCurrency(Money balanceToTransfer) {
        if (!this.balance.currency().equals(balanceToTransfer.currency())) {
            throw new MisMatchCurrencyException();
        }
    }
}
