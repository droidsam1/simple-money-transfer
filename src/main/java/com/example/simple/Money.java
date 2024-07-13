package com.example.simple;

import com.example.simple.exceptions.MismatchCurrencyException;
import java.math.BigDecimal;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    public Money(String amount, String currency) {
        this(BigDecimal.valueOf(Double.parseDouble(amount)), Currency.getInstance(currency));
    }

    public Money subtract(Money funds) {
        validateCurrency(funds);
        return new Money(this.amount.subtract(funds.amount), this.currency);
    }

    private void validateCurrency(Money funds) {
        if (!this.currency().equals(funds.currency())) {
            throw new MismatchCurrencyException();
        }
    }

    public Money add(Money funds) {
        validateCurrency(funds);
        return new Money(this.amount.add(funds.amount), this.currency);
    }
}
