package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import com.example.simple.exceptions.MismatchCurrencyException;
import com.example.simple.exceptions.NegativeTransferAttemptException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
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

    @RepeatedTest(10)
    void shouldTransferMoneyConcurrently() {
        Money initialBalance = dollars(1000);
        Account john = new Account("John Doe", initialBalance);
        Account jane = new Account("Jane Doe", initialBalance);

        List<CompletableFuture<Void>> transfers = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            transfers.add(CompletableFuture.runAsync(() -> john.transfer(jane, dollars(1))));
            transfers.add(CompletableFuture.runAsync(() -> jane.transfer(john, dollars(1))));
        }
        transfers.forEach(CompletableFuture::join);

        Assertions.assertEquals(initialBalance, john.balance());
        Assertions.assertEquals(initialBalance, jane.balance());
    }

    private static Money dollars(int amount) {
        return new Money(String.valueOf(amount), "USD");
    }

}
