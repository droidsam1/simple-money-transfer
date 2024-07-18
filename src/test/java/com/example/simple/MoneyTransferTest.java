package com.example.simple;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

 class MoneyTransferTest {


    @Test
    void shouldTransferMoney() {
        Money initialBalance = dollars(100);
        Account john = new Account("John Doe", initialBalance);
        Account jane = new Account("Jane Doe", initialBalance);

        john.transfer(jane, dollars(50));

        Assertions.assertEquals(dollars(50), john.balance());
        Assertions.assertEquals(dollars(150), jane.balance());

    }

    private static Money dollars(int amount) {
        return new Money(String.valueOf(amount), "USD");
    }

}
