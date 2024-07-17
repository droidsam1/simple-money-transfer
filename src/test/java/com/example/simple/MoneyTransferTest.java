package com.example.simple;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MoneyTransferTest {


    @Test
    void shouldAccountHaveBalance() {
        Money initialBalance = eur(100);
        Account john = new Account("John", initialBalance);

        Assertions.assertEquals(initialBalance, john.balance());
    }

    @Test
    void shouldTransferMoneyBetweenAccounts() {
        Account john = new Account("John", eur(100));
        Account jane = new Account("Jane", eur(0));

        john.transfer(jane, eur(50));

        Assertions.assertEquals(eur(50), john.balance());
        Assertions.assertEquals(eur(50), jane.balance());
    }

    private static Money eur(int amount) {
        return new Money(String.valueOf(amount), "EUR");
    }

}
