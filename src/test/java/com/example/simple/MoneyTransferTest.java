package com.example.simple;

import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MoneyTransferTest {


    @Test
    void shouldAccountsHaveAnInitialBalance() {
        Money initialBalance = dollars(randomAmount());
        Account johnDoe = new Account("John Doe", initialBalance);

        Assertions.assertEquals(initialBalance, johnDoe.balance());
    }

    @Test
    void shouldTransferMoneyToAnotherAccount() {
        Account johnDoe = new Account("John Doe", dollars(100));
        Account janeDoe = new Account("Jane Doe", dollars(0));

        johnDoe.transfer(janeDoe, dollars(100));

        Assertions.assertEquals(dollars(0), johnDoe.balance());
        Assertions.assertEquals(dollars(100), janeDoe.balance());
    }

    private static int randomAmount() {
        return ThreadLocalRandom.current().nextInt(0, 1000);
    }

    private Money dollars(int amount) {
        return new Money(String.valueOf(amount), "USD");
    }

}
