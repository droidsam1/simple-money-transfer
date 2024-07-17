package com.example.simple;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
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


    @RepeatedTest(100)
    void shouldTransferMoneyBetweenAccountsConcurrently() {
        Account john = new Account("John", eur(100));
        Account jane = new Account("Jane", eur(100));

        List<CompletableFuture<Void>> transferTasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            transferTasks.add(CompletableFuture.runAsync(() -> john.transfer(jane, eur(1))));
            transferTasks.add(CompletableFuture.runAsync(() -> jane.transfer(john, eur(1))));
        }
        transferTasks.forEach(CompletableFuture::join);

        Assertions.assertEquals(eur(100), john.balance());
        Assertions.assertEquals(eur(100), jane.balance());
    }

    private static Money eur(int amount) {
        return new Money(String.valueOf(amount), "EUR");
    }

}
