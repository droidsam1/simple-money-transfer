package com.example.simple;

import com.example.simple.exceptions.MisMatchCurrencyException;
import java.math.BigDecimal;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    public Money(String amount, String currency) {
        this(new BigDecimal(amount), Currency.getInstance(currency));
    }

    public Money subtract(Money moneyToWithdraw) {
        validateSameCurrency(moneyToWithdraw);
        return new Money(amount.subtract(moneyToWithdraw.amount), currency);
    }

    private void validateSameCurrency(Money moneyToWithdraw) {
        if (currency != moneyToWithdraw.currency) {
            throw new MisMatchCurrencyException();
        }
    }
}
