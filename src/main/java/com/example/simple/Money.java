package com.example.simple;

import com.example.simple.exceptions.MismatchCurrencyException;
import java.math.BigDecimal;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    public Money(String amount, String currency) {
        this(new BigDecimal(amount), Currency.getInstance(currency));
    }

    public Money subtract(Money funds) {
        validateSameCurrency(funds);
        return new Money(amount.subtract(funds.amount), currency);
    }

    private void validateSameCurrency(Money funds) {
        if (this.currency != funds.currency) {
            throw new MismatchCurrencyException();
        }
    }

    public Money add(Money funds) {
        validateSameCurrency(funds);
        return new Money(amount.add(funds.amount), currency);
    }
}
