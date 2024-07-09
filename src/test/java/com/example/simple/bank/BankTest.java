package com.example.simple.bank;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.example.simple.bank.exceptions.AccountAlreadyRegisteredException;
import com.example.simple.bank.exceptions.AccountNotFoundException;
import com.example.simple.bank.exceptions.InsufficientFundsException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class BankTest {

    //SUT
    private Bank bank;

    @BeforeEach
    public void setUp() {
        this.bank = new Bank();
    }

    @Test
    void shouldRegisterAccounts() {
        Account newAccount = new Account("name", randomAmountOfDollars());

        Assertions.assertDoesNotThrow(() -> this.bank.register(newAccount));
    }

    @Test
    void shouldNotRegisterAccountsTwice() {
        Account newAccount = new Account("name", randomAmountOfDollars());

        this.bank.register(newAccount);

        Assertions.assertThrows(AccountAlreadyRegisteredException.class, () -> this.bank.register(newAccount));
    }

    @Test
    void shouldFetchAccountBalance() {
        Account john = new Account("John", randomAmountOfDollars());
        this.bank.register(john);

        Assertions.assertEquals(john.balance(), this.bank.getBalanceFor(john.id()));
    }

    @Test
    void shouldThrowExceptionWhenAccountNotFound() {
        Account john = new Account("John", randomAmountOfDollars());
        this.bank.register(john);

        AccountId unregisteredAccount = new AccountId("Jane");
        Assertions.assertThrows(AccountNotFoundException.class, () -> this.bank.getBalanceFor(unregisteredAccount));
    }

    @Test
    void shouldTransferMoneyBetweenAccounts() {
        Account john = new Account("John", dollars("1000"));
        Account jane = new Account("Jane", dollars("1000"));
        this.bank.register(john);
        this.bank.register(jane);

        this.bank.transfer(john.id(), jane.id(), dollars("500"));

        Assertions.assertEquals(dollars("500"), this.bank.getBalanceFor(john.id()));
        Assertions.assertEquals(dollars("1500"), this.bank.getBalanceFor(jane.id()));
    }

    @Test
    void shouldFailTransferIfUnknownAccount() {
        Account john = new Account("John", randomAmountOfDollars());
        this.bank.register(john);

        AccountId unregisteredAccount = new AccountId("Jane");
        Assertions.assertThrows(
                AccountNotFoundException.class,
                () -> this.bank.transfer(john.id(), unregisteredAccount, dollars("100"))
        );
    }

    @Test
    void shouldFailToTransferFromAccountWithInsufficientFunds() {
        Account john = new Account("John", dollars("100"));
        Account jane = new Account("Jane", dollars("100"));
        this.bank.register(john);
        this.bank.register(jane);

        Assertions.assertThrows(
                InsufficientFundsException.class,
                () -> this.bank.transfer(john.id(), jane.id(), dollars("200"))
        );
    }

    @RepeatedTest(10)
    void shouldTransferFromMultipleAccountsAndMultipleBanks() {
        Money initialBalance = dollars("100000");
        Account john = new Account("John", initialBalance);
        Account jane = new Account("Jane", initialBalance);
        Account jack = new Account("Jack", initialBalance);
        Account justin = new Account("Justin", initialBalance);
        this.bank.register(john);
        this.bank.register(jane);
        this.bank.register(jack);

        Bank anotherBank = new Bank();
        anotherBank.register(jack);
        anotherBank.register(justin);
        anotherBank.register(john);

        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < initialBalance.amount().intValue(); i++) {

            tasks.add(CompletableFuture.runAsync(() -> this.bank.transfer(john.id(), jane.id(), dollars("1"))));
            tasks.add(CompletableFuture.runAsync(() -> this.bank.transfer(jane.id(), jack.id(), dollars("1"))));
            tasks.add(CompletableFuture.runAsync(() -> anotherBank.transfer(jack.id(), justin.id(), dollars("1"))));
            tasks.add(CompletableFuture.runAsync(() -> anotherBank.transfer(justin.id(), john.id(), dollars("1"))));
        }
        tasks.forEach(CompletableFuture::join);

        Assertions.assertEquals(initialBalance, this.bank.getBalanceFor(john.id()));
        Assertions.assertEquals(initialBalance, this.bank.getBalanceFor(jane.id()));
        Assertions.assertEquals(initialBalance, this.bank.getBalanceFor(jack.id()));
        Assertions.assertEquals(initialBalance, anotherBank.getBalanceFor(justin.id()));
    }

    @RepeatedTest(10)
    void shouldTransferFromMultipleAccountWithinASingleBankAndKeepConsistentBalanceAcrossMultipleBanks() {
        Money initialBalance = dollars("100000");
        Account john = new Account("John", initialBalance);
        Account jane = new Account("Jane", initialBalance);
        Account jack = new Account("Jack", initialBalance);
        Account justin = new Account("Justin", initialBalance);
        this.bank.register(john);
        this.bank.register(jane);
        this.bank.register(jack);

        Bank anotherBank = new Bank();
        anotherBank.register(jack);
        anotherBank.register(justin);
        anotherBank.register(john);

        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        int halfTheInitialBalance = initialBalance.amount()
                                                  .divide(BigDecimal.valueOf(2), RoundingMode.FLOOR)
                                                  .intValue();
        for (int i = 0; i < halfTheInitialBalance; i++) {
            tasks.add(CompletableFuture.runAsync(() -> this.bank.transfer(john.id(), jack.id(), dollars("1"))));
            tasks.add(CompletableFuture.runAsync(() -> this.bank.transfer(john.id(), jane.id(), dollars("1"))));
        }
        tasks.forEach(CompletableFuture::join);
        Assertions.assertEquals(this.bank.getBalanceFor(john.id()), anotherBank.getBalanceFor(john.id()));
        Assertions.assertEquals(dollars("0"), this.bank.getBalanceFor(john.id()));
        Assertions.assertEquals(
                initialBalance.amount().add(BigDecimal.valueOf(halfTheInitialBalance)),
                this.bank.getBalanceFor(jane.id()).amount()
        );
        Assertions.assertEquals(
                initialBalance.amount().add(BigDecimal.valueOf(halfTheInitialBalance)),
                this.bank.getBalanceFor(jack.id()).amount()
        );
        Assertions.assertEquals(this.bank.getBalanceFor(jack.id()), anotherBank.getBalanceFor(jack.id()));
    }


    @Test
    void shouldLeadToIncorrectFinalBalanceDueToRaceCondition() throws InterruptedException {
        Account account1 = new Account("John", dollars("1000000"));
        Account account2 = new Account("jane", dollars("1000000"));
        bank.register(account1);
        bank.register(account2);

        ExecutorService executor = Executors.newFixedThreadPool(10);

        // Transfer from account1 to account2
        Runnable transfer1to2 = () -> bank.transfer(
                account1.id(),
                account2.id(),
                new Money(BigDecimal.valueOf(10), Currency.getInstance("USD"))
        );
        // Transfer from account2 to account1
        Runnable transfer2to1 = () -> bank.transfer(
                account2.id(),
                account1.id(),
                new Money(BigDecimal.valueOf(10), Currency.getInstance("USD"))
        );

        for (int i = 0; i < 1_000_000; i++) {
            executor.execute(transfer1to2);
            executor.execute(transfer2to1);
            executor.execute(transfer1to2);
            executor.execute(transfer2to1);
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        Money finalBalance1 = bank.getBalanceFor(account1.id());
        Money finalBalance2 = bank.getBalanceFor(account2.id());

        // Check if the sum of final balances is not equal to the sum of initial balances
        Assertions.assertEquals(
                account1.balance().amount().add(account2.balance().amount()),
                finalBalance1.amount().add(finalBalance2.amount())
        );
        assertNotEquals(finalBalance1.amount().add(finalBalance2.amount()), BigDecimal.valueOf(2000));
    }


    private Money dollars(String amount) {
        return new Money(new BigDecimal(amount), Currency.getInstance("USD"));
    }

    private Money randomAmountOfDollars() {
        return dollars(ThreadLocalRandom.current().nextInt(1, 1000) + ".00");
    }

}
