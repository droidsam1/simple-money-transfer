package com.example.simple;

import com.example.simple.exceptions.MisMatchCurrencyException;
import java.math.BigDecimal;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    public Money(String amount, String currency) {
        this(new BigDecimal(amount), Currency.getInstance(currency));
    }

    public Money subtract(Money balanceToTransfer) {
        validateCurrency(balanceToTransfer);
        return new Money(amount.subtract(balanceToTransfer.amount()), currency);
    }

    private void validateCurrency(Money balanceToTransfer) {
        if (!this.currency().equals(balanceToTransfer.currency())) {
            throw new MisMatchCurrencyException();
        }
    }

    public Money add(Money balanceToTransfer) {
        validateCurrency(balanceToTransfer);
        return new Money(amount.add(balanceToTransfer.amount()), currency);
    }
}
