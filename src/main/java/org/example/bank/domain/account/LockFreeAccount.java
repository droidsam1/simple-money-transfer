package org.example.bank.domain.account;

import java.util.concurrent.atomic.AtomicReference;
import org.example.bank.domain.exceptions.InsufficientFundsException;
import org.example.bank.domain.exceptions.NegativeTransferAmountException;
import org.example.bank.domain.money.Money;

public final class LockFreeAccount implements Account {

    private final AccountId id;
    private final AtomicReference<Money> balance;

    public LockFreeAccount(AccountId id, Money balance) {
        this.id = id;
        this.balance = new AtomicReference<>(balance);
    }

    @Override
    public LockFreeAccount withdraw(Money amount) {
        validateNonNegativeWithdraw(amount);
        while (true) {
            validateEnoughFunds(amount);
            var expected = this.balance.get();
            if (this.balance.compareAndSet(expected, expected.subtract(amount))) {
                return this;
            }
        }
    }

    private void validateNonNegativeWithdraw(Money amount) {
        if (amount.isNegative()) {
            throw new NegativeTransferAmountException();
        }
    }

    private void validateEnoughFunds(Money amount) {
        if (balance.get().amount().compareTo(amount.amount()) < 0) {
            throw new InsufficientFundsException();
        }
    }

    @Override
    public LockFreeAccount deposit(Money amount) {
        while (true) {
            var expected = this.balance.get();
            if (this.balance.compareAndSet(expected, expected.add(amount))) {
                return this;
            }
        }
    }

    @Override
    public AccountId id() {
        return id;
    }

    @Override
    public boolean compareAndSubtract(Money originBalance, Money moneyToSubtract) {
        validateNonNegativeWithdraw(moneyToSubtract);
        if (!balance.get().equals(originBalance)) {
            return false;
        }
        var expected = this.balance.get();
        validateEnoughFunds(moneyToSubtract);
        return this.balance.compareAndSet(expected, expected.subtract(moneyToSubtract));
    }

    @Override
    public boolean compareAndAdd(Money originBalance, Money moneyToAdd) {
        if (!balance.get().equals(originBalance)) {
            return false;
        }
        var expected = this.balance.get();
        return this.balance.compareAndSet(expected, expected.add(moneyToAdd));
    }

    public Money balance() {
        return balance.get();
    }
}
