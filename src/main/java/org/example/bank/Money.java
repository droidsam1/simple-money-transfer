package org.example.bank;

import java.math.BigDecimal;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    public Money(String amount, String currency) {
        this(new BigDecimal(amount), Currency.getInstance(currency));
    }
}
