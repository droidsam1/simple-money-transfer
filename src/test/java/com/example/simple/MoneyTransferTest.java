package com.example.simple;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MoneyTransferTest {


    @Test
    void shouldTransferMoney() {
        Money initialBalance = euros(100);
        Account john = new Account("John", initialBalance);
        Account jane = new Account("Jane", initialBalance);

        john.transfer(jane, euros(50));

        Assertions.assertEquals(euros(50), john.getBalance());
        Assertions.assertEquals(euros(150), jane.getBalance());
    }

    private static Money euros(int amount) {
        return new Money(String.valueOf(amount), "EUR");
    }

}
