package com.example.simple.bank;

import com.example.simple.bank.exceptions.AccountAlreadyRegisteredException;
import com.example.simple.bank.exceptions.AccountNotFoundException;
import com.example.simple.bank.exceptions.InsufficientFundsException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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

    @Test
    void shouldTransferFromMultipleAccounts() {
        Account john = new Account("John", dollars("1000"));
        Account jane = new Account("Jane", dollars("1000"));
        Account jack = new Account("Jack", dollars("1000"));
        this.bank.register(john);
        this.bank.register(jane);
        this.bank.register(jack);

        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            tasks.add(CompletableFuture.runAsync(() -> this.bank.transfer(john.id(), jane.id(), dollars("1"))));
            tasks.add(CompletableFuture.runAsync(() -> this.bank.transfer(jane.id(), jack.id(), dollars("1"))));
            tasks.add(CompletableFuture.runAsync(() -> this.bank.transfer(jack.id(), john.id(), dollars("1"))));
        }
        tasks.forEach(CompletableFuture::join);

        Assertions.assertEquals(dollars("1000"), this.bank.getBalanceFor(john.id()));
        Assertions.assertEquals(dollars("1000"), this.bank.getBalanceFor(jane.id()));
        Assertions.assertEquals(dollars("1000"), this.bank.getBalanceFor(jack.id()));
    }

    private Money dollars(String amount) {
        return new Money(new BigDecimal(amount), Currency.getInstance("USD"));
    }

    private Money randomAmountOfDollars() {
        return dollars(ThreadLocalRandom.current().nextInt(1, 1000) + ".00");
    }

}
