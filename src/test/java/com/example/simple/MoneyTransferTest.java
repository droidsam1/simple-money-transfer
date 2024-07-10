package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import com.example.simple.exceptions.NegativeTransferAmountException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MoneyTransferTest {


    @Test
    void shouldRegisterAnAccount() {
        Money anInitialBalance = dollars(100);

        Assertions.assertDoesNotThrow(() -> new Account("John", anInitialBalance));
    }

    @Test
    void shouldAccountHaveABalance() {
        Account john = new Account("john", dollars(1000));

        Assertions.assertEquals(dollars(1000), john.balance());
    }

    @Test
    void shouldNotTransferIfSenderHasNotEnoughFunds() {
        Account john = new Account("john", dollars(50));
        Account jane = new Account("Jane", dollars(0));

        Assertions.assertThrows(InsufficientFundsException.class, () -> john.transferTo(jane, dollars(100)));
    }

    @Test
    void shouldFailWhenTransferFundsAreNegative() {
        Account john = new Account("john", dollars(50));
        Account jane = new Account("Jane", dollars(0));

        Assertions.assertThrows(NegativeTransferAmountException.class, () -> john.transferTo(jane, dollars(-1)));
    }

    @Test
    void shouldTransferToAccount() {
        Account john = new Account("john", dollars(50));
        Account jane = new Account("Jane", dollars(0));

        john.transferTo(jane, dollars(50));

        Assertions.assertEquals(dollars(0), john.balance());
        Assertions.assertEquals(dollars(50), jane.balance());
    }

    @Test
    void shouldTransferMultipleAccounts() {
        int initialBalance = 1000;
        Account john = new Account("john", dollars(initialBalance));
        Account jane = new Account("Jane", dollars(initialBalance));
        Account jack = new Account("Jack", dollars(initialBalance));
        Account james = new Account("James", dollars(initialBalance));

        List<CompletableFuture<Void>> transfersTasks = new ArrayList<>();
        for (int i = 0; i < initialBalance; i++) {
            transfersTasks.add(CompletableFuture.runAsync(() -> john.transferTo(jane, dollars(1))));
            transfersTasks.add(CompletableFuture.runAsync(() -> john.transferTo(james, dollars(1))));
            transfersTasks.add(CompletableFuture.runAsync(() -> jane.transferTo(jack, dollars(1))));
            transfersTasks.add(CompletableFuture.runAsync(() -> jack.transferTo(john, dollars(1))));
            transfersTasks.add(CompletableFuture.runAsync(() -> james.transferTo(john, dollars(1))));
        }
        transfersTasks.forEach(CompletableFuture::join);

        Assertions.assertEquals(dollars(initialBalance), john.balance());
        Assertions.assertEquals(dollars(initialBalance), jane.balance());
        Assertions.assertEquals(dollars(initialBalance), jack.balance());
    }


    private static Money dollars(int amount) {
        return new Money(amount, "USD");
    }


}
