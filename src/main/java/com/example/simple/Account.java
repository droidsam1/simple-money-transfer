package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import com.example.simple.exceptions.NegativeAmountTransferException;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public class Account {

    private final AccountId id;
    private final AtomicReference<Money> balance;


    public Account(String id, Money balance) {
        this.id = new AccountId(id);
        this.balance = new AtomicReference<>(balance);
    }

    public Money getBalance() {
        return balance.get();
    }

    public void transfer(Account recipient, Money funds) {
        validateFundsArePositive(funds);

        while (true) {
            Money expectedSenderBalance = this.balance.get();
            Money expectedRecipientBalance = recipient.getBalance();

            if (this.balance.compareAndSet(expectedSenderBalance, expectedSenderBalance.subtract(funds))) {
                if (recipient.balance.compareAndSet(expectedRecipientBalance, expectedRecipientBalance.add(funds))) {
                    break;
                } else {
                    //rollback
                    this.deposit(funds);
                }
            }
        }
    }

    private void deposit(Money funds) {
        this.balance.updateAndGet(b -> b.add(funds));
    }

    private void withdraw(Money funds) {
        this.balance.updateAndGet(b -> {
            if (b.amount().compareTo(funds.amount()) < 0) {
                throw new InsufficientFundsException();
            }
            return b.subtract(funds);
        });
    }

    private void validateFundsArePositive(Money funds) {
        if (funds.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeAmountTransferException();
        }
    }
}
