package com.example.simple;

import com.example.simple.exceptions.MisMatchCurrencyException;
import java.math.BigDecimal;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {


    public Money(String amount, String currency) {
        this(new BigDecimal(amount), Currency.getInstance(currency));
    }

    public Money subtract(Money transferFunds) {
        validateSameCurrency(transferFunds);
        return new Money(this.amount.subtract(transferFunds.amount), this.currency);
    }

    private void validateSameCurrency(Money transferFunds) {
        if (this.currency != transferFunds.currency) {
            throw new MisMatchCurrencyException();
        }
    }

    public Money add(Money transferFunds) {
        validateSameCurrency(transferFunds);
        return new Money(this.amount.add(transferFunds.amount), this.currency);
    }
}
