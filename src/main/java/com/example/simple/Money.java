package com.example.simple;

import java.math.BigDecimal;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    public Money(int amount, String currency) {
        this(BigDecimal.valueOf(amount), Currency.getInstance(currency));
    }
}
