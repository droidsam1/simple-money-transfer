package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import com.example.simple.exceptions.MismatchCurrencyException;
import com.example.simple.exceptions.NegativeTransferAmountException;
import java.math.BigDecimal;

public class Account {

    private final AccountId id;
    private Money balance;

    public Account(AccountId id, Money balance) {
        this.id = id;
        this.balance = balance;
    }

    public Account(String id, Money balance) {
        this(new AccountId(id), balance);
    }

    public Money balance() {
        return balance;
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

    private void deposit(Money transferFunds) {
        this.balance = this.balance.add(transferFunds);
    }

    private void withdraw(Money transferFunds) {
        this.balance = this.balance.subtract(transferFunds);
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
