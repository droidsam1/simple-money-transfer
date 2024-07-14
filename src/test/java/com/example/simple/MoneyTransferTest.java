package com.example.simple;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MoneyTransferTest {


    @Test
    void shouldTransferMoney() {
        Money initialBalance = dollars(100);
        Account johnDoe = new Account("John Doe", initialBalance);
        Account janeDoe = new Account("Jane Doe", initialBalance);

        johnDoe.transfer(janeDoe, dollars(100));

        Assertions.assertEquals(dollars(200), janeDoe.balance());
        Assertions.assertEquals(dollars(0), johnDoe.balance());
    }

    private static Money dollars(int amount) {
        return new Money(String.valueOf(amount), "USD");
    }

}
