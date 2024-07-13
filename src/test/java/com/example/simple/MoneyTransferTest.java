package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import com.example.simple.exceptions.MismatchCurrencyException;
import com.example.simple.exceptions.NegativeTransferAmountException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class MoneyTransferTest {


    @Test
    void shouldCreateAccounts() {
        Money initialBalance = dollars(10);
        Assertions.assertDoesNotThrow(() -> new Account("John", initialBalance));
    }

    @Test
    void shouldTransferMoney() {
        Account john = new Account("John", dollars(10));
        Account jane = new Account("Jane", dollars(0));

        john.transfer(jane, dollars(10));

        Assertions.assertEquals(dollars(0), john.balance());
        Assertions.assertEquals(dollars(10), jane.balance());
    }

    @Test
    void shouldNotTransferMoneyWhenNotEnoughMoney() {
        Account john = new Account("John", dollars(10));
        Account jane = new Account("Jane", dollars(0));

        Assertions.assertThrows(InsufficientFundsException.class, () -> john.transfer(jane, dollars(100)));
    }

    @Test
    void shouldNotTransferMoneyWhenTransferIsNegative() {
        Account john = new Account("John", dollars(10));
        Account jane = new Account("Jane", dollars(0));

        Assertions.assertThrows(NegativeTransferAmountException.class, () -> john.transfer(jane, dollars(-1)));
    }

    @Test
    void shouldNotTransferMoneyWhenTransferIsDifferentCurrency() {
        Account john = new Account("John", dollars(10));
        Account jane = new Account("Jane", new Money("0", "EUR"));

        Assertions.assertThrows(MismatchCurrencyException.class, () -> john.transfer(jane, dollars(1)));
    }

    @Test
    void shouldTransferMoneyFromDifferentAccount() {
        Account john = new Account("John", dollars(10));
        Account jane = new Account("Jane", dollars(0));

        john.transfer(jane, dollars(10));
        jane.transfer(john, dollars(10));

        Assertions.assertEquals(dollars(10), john.balance());
        Assertions.assertEquals(dollars(0), jane.balance());
    }

    @RepeatedTest(1000)
    void shouldTransferMoneyConcurrently() {
        Account john = new Account("John", dollars(1000));
        Account jane = new Account("Jane", dollars(1000));
        Account jack = new Account("Jack", dollars(1000));

        List<CompletableFuture<Void>> transferTasks = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            transferTasks.add(CompletableFuture.runAsync(() -> john.transfer(jane, dollars(1))));
            transferTasks.add(CompletableFuture.runAsync(() -> jane.transfer(jack, dollars(1))));
            transferTasks.add(CompletableFuture.runAsync(() -> jack.transfer(john, dollars(1))));

        }
        transferTasks.forEach(CompletableFuture::join);

        Assertions.assertEquals(dollars(1000), john.balance());
        Assertions.assertEquals(dollars(1000), jane.balance());
        Assertions.assertEquals(dollars(1000), jack.balance());
    }


    private static Money dollars(int amount) {
        return new Money(String.valueOf(amount), "USD");
    }

}
