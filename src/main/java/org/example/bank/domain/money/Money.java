package org.example.bank.domain.money;

import java.math.BigDecimal;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    public Money(int amount, String currency) {
        this(BigDecimal.valueOf(amount), Currency.getInstance(currency));
    }

    public Money subtract(int subtrahend) {
        return new Money(this.amount.subtract(BigDecimal.valueOf(subtrahend)), this.currency);
    }

    public Money add(int addend) {
        return new Money(this.amount.add(BigDecimal.valueOf(addend)), this.currency);
    }

    public Money subtract(Money subtrahend) {
        if (!this.currency.equals(subtrahend.currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        return new Money(this.amount.subtract(subtrahend.amount), this.currency);
    }

    public Money add(Money summand) {
        if (!this.currency.equals(summand.currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        return new Money(this.amount.add(summand.amount), this.currency);
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }
}