package org.example.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MoneyTest {


    @Test
    void shouldCreateMoney() {
        Money money = new Money("1000", "USD");
        assertEquals("1000", money.amount().toString());
        assertEquals("USD", money.currency().getCurrencyCode());
    }

    @Test
    void shouldFailToCreateMoneyWithInvalidCurrency() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Money("1000", "US");
        });
    }

    @Test
    void shouldFailToCreateMoneyWithInvalidAmount() {
        Assertions.assertThrows(NumberFormatException.class, () -> {
            new Money("", "USD");
        });
    }
}