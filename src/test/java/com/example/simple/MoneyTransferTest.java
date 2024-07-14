package com.example.simple;

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
        Account johnDoe = new Account("John Doe", initialBalance);
        Account janeDoe = new Account("Jane Doe", initialBalance);

        johnDoe.transfer(janeDoe, dollars(100));

        Assertions.assertEquals(dollars(200), janeDoe.balance());
        Assertions.assertEquals(dollars(0), johnDoe.balance());
    }

    @RepeatedTest(100)
    void shouldTransferMoneyConcurrently() {
        Money initialBalance = dollars(1000);
        Account johnDoe = new Account("John Doe", initialBalance);
        Account janeDoe = new Account("Jane Doe", initialBalance);

        List<CompletableFuture<Void>> transfersTasks = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            transfersTasks.add(CompletableFuture.runAsync(() -> johnDoe.transfer(janeDoe, dollars(1))));
            transfersTasks.add(CompletableFuture.runAsync(() -> janeDoe.transfer(johnDoe, dollars(1))));
        }
        transfersTasks.forEach(CompletableFuture::join);

        Assertions.assertEquals(dollars(1000), janeDoe.balance());
        Assertions.assertEquals(dollars(1000), johnDoe.balance());
    }

    private static Money dollars(int amount) {
        return new Money(String.valueOf(amount), "USD");
    }

}
