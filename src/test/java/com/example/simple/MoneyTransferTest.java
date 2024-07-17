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
        Money initialBalance = euros(100);
        Account john = new Account("John", initialBalance);
        Account jane = new Account("Jane", initialBalance);

        john.transfer(jane, euros(50));

        Assertions.assertEquals(euros(50), john.getBalance());
        Assertions.assertEquals(euros(150), jane.getBalance());
    }


    @RepeatedTest(10)
    void shouldTransferMoneyConcurrently() {
        Money initialBalance = euros(1000);
        Account john = new Account("John", initialBalance);
        Account jane = new Account("Jane", initialBalance);

        List<CompletableFuture<Void>> transfers = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            transfers.add(CompletableFuture.runAsync(() -> john.transfer(jane, euros(1))));
            transfers.add(CompletableFuture.runAsync(() -> jane.transfer(john, euros(1))));
        }
        transfers.forEach(CompletableFuture::join);

        Assertions.assertEquals(initialBalance, john.getBalance());
        Assertions.assertEquals(initialBalance, jane.getBalance());
    }

    private static Money euros(int amount) {
        return new Money(String.valueOf(amount), "EUR");
    }

}
