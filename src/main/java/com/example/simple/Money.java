package com.example.simple;

import com.example.simple.exceptions.MisMatchCurrencyException;
import java.math.BigDecimal;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    public Money(String amount, String currency) {
        this(new BigDecimal(amount), Currency.getInstance(currency));
    }

    public Money subtract(Money funds) {
        validateSameCurrency(funds);
        return new Money(this.amount.subtract(funds.amount), this.currency);
    }

    private void validateSameCurrency(Money funds) {
        if (funds.currency != currency) {
            throw new MisMatchCurrencyException();
        }
    }

    public Money add(Money funds) {
        validateSameCurrency(funds);
        return new Money(this.amount.add(funds.amount), this.currency);
    }
}
