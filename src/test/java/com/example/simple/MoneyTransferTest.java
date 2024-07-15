package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MoneyTransferTest {

    //transfer = withdraw and deposit

    @Test
    void shouldAccountsHaveBalance() {
        Money initialBalance = dollars(100);
        Account john = new Account("John", initialBalance);

        Assertions.assertEquals(initialBalance, john.balance());
    }

    @Test
    void shouldWithdrawFunds() {
        Account john = new Account("John", dollars(100));

        john.withdraw(dollars(50));

        Assertions.assertEquals(dollars(50), john.balance());
    }

    @Test
    void shouldNoWithdrawFundsWhenInsufficientFunds() {
        Account john = new Account("John", dollars(0));

        Assertions.assertThrows(InsufficientFundsException.class, () -> john.withdraw(dollars(100)));

        Assertions.assertEquals(dollars(0), john.balance());
    }

    @Test
    void shouldDepositFunds() {
        Account john = new Account("John", dollars(100));

        john.deposit(dollars(50));

        Assertions.assertEquals(dollars(150), john.balance());
    }


    @Test
    void shouldTransferMoney() {
        Account john = new Account("John", dollars(100));
        Account jane = new Account("Jane", dollars(0));

        john.transfer(jane, dollars(50));

        Assertions.assertEquals(dollars(50), john.balance());
        Assertions.assertEquals(dollars(50), jane.balance());
    }

    @Test
    void shouldTransferMoneyConcurrently() {
        Account john = new Account("John", dollars(1000));
        Account jane = new Account("Jane", dollars(1000));

        List<CompletableFuture<Void>> transferTasks = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            transferTasks.add(CompletableFuture.runAsync(() -> john.transfer(jane, dollars(1))));
            transferTasks.add(CompletableFuture.runAsync(() -> jane.transfer(john, dollars(1))));
        }
        transferTasks.forEach(CompletableFuture::join);

        Assertions.assertEquals(dollars(1000), john.balance());
        Assertions.assertEquals(dollars(1000), jane.balance());
    }


    private Money dollars(int amount) {
        return new Money(String.valueOf(amount), "USD");
    }

}
