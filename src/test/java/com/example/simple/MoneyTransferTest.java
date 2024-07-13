package com.example.simple;

import com.example.simple.exceptions.InsufficientFundsException;
import com.example.simple.exceptions.MismatchCurrencyException;
import com.example.simple.exceptions.NegativeTransferAmountException;
import org.junit.jupiter.api.Assertions;
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
    void shouldTransferMoneyFromDifferentAccount(){
        Account john = new Account("John", dollars(10));
        Account jane = new Account("Jane", dollars(0));

        john.transfer(jane, dollars(10));
        jane.transfer(john, dollars(10));

        Assertions.assertEquals(dollars(10), john.balance());
        Assertions.assertEquals(dollars(0), jane.balance());


    }


    private static Money dollars(int amount) {
        return new Money(String.valueOf(amount), "USD");
    }

}
