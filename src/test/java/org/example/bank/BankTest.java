package org.example.bank;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.bank.exceptions.InsufficientFundsException;
import org.example.bank.exceptions.InvalidTransferAmountException;
import org.example.bank.exceptions.UnknownAccountException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
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
        int initialBalance = 1_000;
        Account johnDoe = new Account("John Doe", dollars(String.valueOf(initialBalance)));
        Account janeDoe = new Account("Jane Doe", dollars(String.valueOf(initialBalance)));
        Account jackDoe = new Account("Jack Doe", dollars(String.valueOf(initialBalance)));
        this.bank.register(johnDoe);
        this.bank.register(janeDoe);
        this.bank.register(jackDoe);

        List<CompletableFuture<Void>> transfers = new ArrayList<>();
        for (int i = 0; i < initialBalance; i++) {
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

        Assertions.assertEquals(dollars(String.valueOf(initialBalance)), this.bank.getBalance(johnDoe.id()));
        Assertions.assertEquals(dollars(String.valueOf(initialBalance)), this.bank.getBalance(janeDoe.id()));
        Assertions.assertEquals(dollars(String.valueOf(initialBalance)), this.bank.getBalance(jackDoe.id()));
    }


    @Test
    void shouldBalanceBeCorrectInConcurrentScenario() {
        final Account sharedAccount = new Account("Shared Account", new Money("1000", "USD"));
        AtomicInteger totalDeposited = new AtomicInteger(0);
        AtomicInteger totalWithdrawn = new AtomicInteger(0);
        List<CompletableFuture<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < 10000; i++) {
            CompletableFuture<Void> depositTask = CompletableFuture.runAsync(() -> {
                sharedAccount.deposit(new Money("1", "USD"));
                totalDeposited.addAndGet(1);
            });
            CompletableFuture<Void> withdrawTask = CompletableFuture.runAsync(() -> {
                try {
                    sharedAccount.withdraw(new Money("1", "USD"));
                    totalWithdrawn.addAndGet(1);
                } catch (InsufficientFundsException e) {
                    // do nothing
                }
            });
            tasks.add(depositTask);
            tasks.add(withdrawTask);
        }
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();

        int expectedBalance = 1000 + totalDeposited.get() - totalWithdrawn.get();
        assertEquals(dollars(String.valueOf(expectedBalance)), sharedAccount.balance());
    }

    @RepeatedTest(1000)
    void shouldTransferBeAtomic() {
        AtomicInteger beforeBetweenOrAfter = new AtomicInteger(ThreadLocalRandom.current().nextInt(1, 3));
        this.bank = new Bank(forceTransferToFailRandomly(beforeBetweenOrAfter));

        Money initialBalance = dollars("1000");
        Account johnDoe = new Account("John Doe", initialBalance);
        Account janeDoe = new Account("Jane Doe", initialBalance);
        this.bank.register(johnDoe);
        this.bank.register(janeDoe);

        Money transferAmount = dollars("500");
        try {
            this.bank.transfer(johnDoe.id(), janeDoe.id(), transferAmount);
        } catch (Exception e) {
            //ignore
        }

        Assertions.assertEquals(initialBalance, this.bank.getBalance(johnDoe.id()));
        Assertions.assertEquals(initialBalance, this.bank.getBalance(janeDoe.id()));
    }

    private static Runnable forceTransferToFailRandomly(AtomicInteger failAfter) {
        return () -> {
            if (failAfter.decrementAndGet() == 0) {
                throw new RuntimeException("In between transfer behaviour");
            }
        };
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
