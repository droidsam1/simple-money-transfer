package com.example.simple;

import com.example.simple.exceptions.MisMatchCurrency;
import java.math.BigDecimal;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    public Money(int amount, String currency) {
        this(BigDecimal.valueOf(amount), Currency.getInstance(currency));
    }

    public Money subtract(Money anAmount) {
        validateCurrency(anAmount);
        return new Money(this.amount.subtract(anAmount.amount()), this.currency);
    }

    public Money add(Money anAmount) {
        validateCurrency(anAmount);
        return new Money(this.amount.add(anAmount.amount()), this.currency);
    }

    private void validateCurrency(Money another) {
        if (!this.currency().equals(another.currency())) {
            throw new MisMatchCurrency();
        }
    }
}
