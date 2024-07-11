package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import com.example.simple.exceptions.MisMatchCurrencyException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Account {

    private final String id;
    private final AtomicReference<Money> balance;

    public Account(String id, Money balance) {
        this.id = Objects.requireNonNull(id);
        this.balance = new AtomicReference<>(Objects.requireNonNull(balance));
    }

    public Money balance() {
        return this.balance.get();
    }

    public void transfer(Account beneficiary, Money balanceToTransfer) {
        validateCurrency(balanceToTransfer);
        validateBalanceToTransferIsPositive(balanceToTransfer);
        validateEnoughFunds(balanceToTransfer);

        //atomic operation
        optimisticTransferStrategy(beneficiary, balanceToTransfer);
    }

    private void optimisticTransferStrategy(Account beneficiary, Money balanceToTransfer) {
        boolean withdraw;
        boolean deposit;

        while (true) {
            var expectedSenderBalance = this.balance.get();
            var expectedBeneficiaryBalance = beneficiary.balance.get();

            withdraw = this.balance.compareAndSet(
                    expectedSenderBalance,
                    expectedSenderBalance.subtract(balanceToTransfer)
            );
            if (withdraw) {
                deposit = beneficiary.balance.compareAndSet(
                        expectedBeneficiaryBalance,
                        expectedBeneficiaryBalance.add(balanceToTransfer)
                );
                if (deposit) {
                    return;
                } else {
                    //rollback: naive implementation
                    this.deposit(balanceToTransfer);
                }
            }
        }
    }

    private void pessimisticTransferStrategy(Account beneficiary, Money balanceToTransfer) {
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
        this.balance.updateAndGet(b -> b.add(balanceToTransfer));
    }

    private void withdraw(Money balanceToTransfer) {
        this.balance.updateAndGet(b -> b.subtract(balanceToTransfer));
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
        if (!this.balance.get().currency().equals(balanceToTransfer.currency())) {
            throw new MisMatchCurrencyException();
        }
    }
}
