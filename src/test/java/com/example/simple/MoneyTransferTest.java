package com.example.simple;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MoneyTransferTest {


    @Test
    void shouldTransferMoneyFromAccountToAccount() {
        Money initialBalance = dollars(100);
        Account john = new Account("John", initialBalance);
        Account jane = new Account("Jane", dollars(0));

        john.transfer(jane, dollars(50));

        Assertions.assertEquals(dollars(50), john.balance());
        Assertions.assertEquals(dollars(50), jane.balance());
    }

    @Test
    void shouldTransferMoneyFromDifferentAccounts() {
        Account john = new Account("John", dollars(100));
        Account jane = new Account("Jane", dollars(100));
        Account jack = new Account("Jane", dollars(100));

        List<CompletableFuture<Void>> transfers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            transfers.add(CompletableFuture.runAsync(() -> john.transfer(jane, dollars(1))));
            transfers.add(CompletableFuture.runAsync(() -> jane.transfer(jack, dollars(1))));
            transfers.add(CompletableFuture.runAsync(() -> jack.transfer(john, dollars(1))));
        }
        transfers.forEach(CompletableFuture::join);

        Assertions.assertEquals(dollars(100), john.balance());
        Assertions.assertEquals(john.balance(), jane.balance());
        Assertions.assertEquals(jane.balance(), jack.balance());
    }

    private Money dollars(int amount) {
        return new Money(amount, "USD");
    }

}
