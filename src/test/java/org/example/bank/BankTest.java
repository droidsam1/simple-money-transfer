package org.example.bank;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.example.bank.exceptions.InvalidTransferAmountException;
import org.example.bank.exceptions.UnknownAccountException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BankTest {


    //SUT
    private Bank bank;

    @BeforeEach
    void setUp() {
        bank = new Bank();
    }

    @Test
    void shouldRegisterNewAccounts() {
        Account johnDoe = new Account("John Doe", randomPositiveMoney());

        assertDoesNotThrow(() -> this.bank.register(johnDoe));
    }

    @Test
    void shouldNotRegisterNewClientsWithNegativeBalance() {
        Account johnDoe = new Account("John Doe", randomNegativeMoney());

        assertThrows(IllegalArgumentException.class, () -> this.bank.register(johnDoe));
    }

    @Test
    void shouldFetchAccountBalance() {
        Account johnDoe = new Account("John Doe", randomPositiveMoney());
        this.bank.register(johnDoe);

        Money balance = this.bank.getBalance(johnDoe.id());

        assertEquals(johnDoe.balance(), balance);
    }

    @Test
    void shouldFailWhenFetchingBalanceOfUnknownAccount() {
        Account unknownAccount = new Account("Unknown", randomPositiveMoney());

        assertThrows(UnknownAccountException.class, () -> this.bank.getBalance(unknownAccount.id()));
    }

    @Test
    void shouldNoRegisterSameAccountTwice() {
        Account johnDoe = new Account("John Doe", randomPositiveMoney());
        this.bank.register(johnDoe);

        assertThrows(IllegalArgumentException.class, () -> this.bank.register(johnDoe));
    }


    @Test
    void shouldTransferFromAccountToAccount() {
        Money initialBalance = dollars("1000");
        Account johnDoe = new Account("John Doe", initialBalance);
        Account janeDoe = new Account("Jane Doe", initialBalance);
        this.bank.register(johnDoe);
        this.bank.register(janeDoe);

        Money transferAmount = dollars("500");
        this.bank.transfer(johnDoe.id(), janeDoe.id(), transferAmount);

        assertEquals(
                initialBalance.amount().subtract(transferAmount.amount()),
                this.bank.getBalance(johnDoe.id()).amount()
        );
        assertEquals(initialBalance.amount().add(transferAmount.amount()), this.bank.getBalance(janeDoe.id()).amount());
    }

    @Test
    void shouldFailToTransferNegativeAmount() {
        Money initialBalance = dollars("1000");
        Account johnDoe = new Account("John Doe", initialBalance);
        Account janeDoe = new Account("Jane Doe", initialBalance);
        this.bank.register(johnDoe);
        this.bank.register(janeDoe);

        Money transferAmount = dollars("-500");

        assertThrows(
                InvalidTransferAmountException.class,
                () -> this.bank.transfer(johnDoe.id(), janeDoe.id(), transferAmount)
        );
    }

    @Test
    void shouldConcurrentlyTransfer() {
        Money initialBalance = dollars("1000");
        Account johnDoe = new Account("John Doe", initialBalance);
        Account janeDoe = new Account("Jane Doe", initialBalance);
        Account jackDoe = new Account("Jack Doe", initialBalance);
        this.bank.register(johnDoe);
        this.bank.register(janeDoe);
        this.bank.register(jackDoe);

        List<CompletableFuture<Void>> transfers = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            transfers.add(CompletableFuture.runAsync(() -> this.bank.transfer(
                    johnDoe.id(),
                    janeDoe.id(),
                    dollars("1")
            )));
            transfers.add(CompletableFuture.runAsync(() -> this.bank.transfer(
                    janeDoe.id(),
                    jackDoe.id(),
                    dollars("1")
            )));
            transfers.add(CompletableFuture.runAsync(() -> this.bank.transfer(
                    jackDoe.id(),
                    johnDoe.id(),
                    dollars("1")
            )));
        }
        transfers.forEach(CompletableFuture::join);

        Assertions.assertEquals(initialBalance, this.bank.getBalance(johnDoe.id()));
        Assertions.assertEquals(initialBalance, this.bank.getBalance(janeDoe.id()));
        Assertions.assertEquals(initialBalance, this.bank.getBalance(jackDoe.id()));
    }

    private Money dollars(String amount) {
        return new Money(amount, "USD");
    }


    private Money randomPositiveMoney() {
        return new Money(randomAmount(), randomCurrency());
    }

    private BigDecimal randomAmount() {
        return BigDecimal.valueOf(Math.random() * 1000);
    }

    private Currency randomCurrency() {
        return Currency.getAvailableCurrencies()
                       .stream()
                       .toList()
                       .get((int) (Math.random() * Currency.getAvailableCurrencies().size()));

    }

    private Money randomNegativeMoney() {
        return new Money(randomAmount().negate(), randomCurrency());
    }

}
