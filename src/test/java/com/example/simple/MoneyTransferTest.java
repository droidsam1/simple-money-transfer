package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import com.example.simple.exceptions.MismatchCurrencyException;
import com.example.simple.exceptions.NegativeTransferAttemptException;
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

    @Test
    void shouldFailWhenInsufficientFunds() {
        Money initialBalance = dollars(100);
        Account john = new Account("John Doe", initialBalance);
        Account jane = new Account("Jane Doe", initialBalance);

        Assertions.assertThrows(InsufficientFundsException.class, () -> john.transfer(jane, dollars(150)));

        Assertions.assertEquals(initialBalance, john.balance());
        Assertions.assertEquals(initialBalance, jane.balance());
    }

    @Test
    void shouldFailWhenAttemptToTransferNegativeAmount() {
        Money initialBalance = dollars(100);
        Account john = new Account("John Doe", initialBalance);
        Account jane = new Account("Jane Doe", initialBalance);

        Assertions.assertThrows(NegativeTransferAttemptException.class, () -> john.transfer(jane, dollars(-1)));
    }

    @Test
    void shouldFailWhenTransferWithDifferentCurrency() {
        Money initialBalance = dollars(100);
        Account john = new Account("John Doe", new Money("100", "EUR"));
        Account jane = new Account("Jane Doe", initialBalance);

        Assertions.assertThrows(MismatchCurrencyException.class, () -> john.transfer(jane, dollars(10)));
    }

    private static Money dollars(int amount) {
        return new Money(String.valueOf(amount), "USD");
    }

}
