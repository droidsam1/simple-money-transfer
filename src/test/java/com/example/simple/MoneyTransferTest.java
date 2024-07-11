package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MoneyTransferTest {


    @Test
    void shouldAccountHaveBalance() {
        Money initialBalance = dollars(randomAmount());

        Account john = new Account("John", initialBalance);

        Assertions.assertEquals(initialBalance, john.balance());
    }

    @Test
    void shouldNoCreateAccountsWithoutId() {
        Assertions.assertThrows(NullPointerException.class, () -> new Account(null, dollars(randomAmount())));
    }

    @Test
    void shouldTransferFromAccountToAccount() {
        Money initialBalance = dollars(50);
        Account john = new Account("John", initialBalance);
        Account jane = new Account("Jane", dollars(0));

        john.transfer(jane, dollars(10));

        Assertions.assertEquals(dollars(10), jane.balance());
        Assertions.assertEquals(dollars(40), john.balance());
    }

    @Test
    void shouldNotTransferNegativeAmounts() {
        Money initialBalance = dollars(50);
        Account john = new Account("John", initialBalance);
        Account jane = new Account("Jane", dollars(0));

        Assertions.assertThrows(NegativeTransferException.class, () -> john.transfer(jane, dollars(-10)));

        Assertions.assertEquals(dollars(0), jane.balance());
        Assertions.assertEquals(dollars(50), john.balance());
    }

    @Test
    void shouldNotTransferWhenNotEnoughFunds() {
        Money initialBalance = dollars(50);
        Account john = new Account("John", initialBalance);
        Account jane = new Account("Jane", dollars(0));

        Assertions.assertThrows(InsufficientFundsException.class, () -> john.transfer(jane, dollars(100)));

        Assertions.assertEquals(dollars(0), jane.balance());
        Assertions.assertEquals(dollars(50), john.balance());
    }


    @Test
    void shouldTransferFromAccountToAccountConcurrently() {
        Money initialBalance = dollars(5000);

        Account john = new Account("John", initialBalance);
        Account jane = new Account("Jane", initialBalance);
        Account jack = new Account("Jack", initialBalance);

        List<CompletableFuture<Void>> transfersTasks = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            transfersTasks.add(CompletableFuture.runAsync(() -> john.transfer(jane, dollars(1))));
            transfersTasks.add(CompletableFuture.runAsync(() -> jane.transfer(jack, dollars(1))));
            transfersTasks.add(CompletableFuture.runAsync(() -> jack.transfer(john, dollars(1))));
        }
        transfersTasks.forEach(CompletableFuture::join);

        Assertions.assertEquals(initialBalance, jane.balance());
        Assertions.assertEquals(initialBalance, john.balance());
        Assertions.assertEquals(initialBalance, jack.balance());
    }

    private int randomAmount() {
        return ThreadLocalRandom.current().nextInt(0, 1000);
    }

    private Money dollars(int amount) {
        return new Money(String.valueOf(amount), "USD");
    }

}
