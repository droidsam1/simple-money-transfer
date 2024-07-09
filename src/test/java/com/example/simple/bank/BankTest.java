package com.example.simple.bank;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.example.simple.bank.exceptions.AccountAlreadyRegisteredException;
import com.example.simple.bank.exceptions.AccountNotFoundException;
import com.example.simple.bank.exceptions.InsufficientFundsException;
import com.example.simple.bank.repository.AccountRepository;
import com.example.simple.bank.repository.optimistic.OptimisticAccountRepository;
import com.example.simple.bank.repository.pessimistic.GlobalLockManagerAccountRepository;
import com.example.simple.bank.repository.pessimistic.SingleLockAccountRepository;
import com.example.simple.bank.repository.pessimistic.SynchronizationOnAccountsAccountRepository;
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
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class BankTest {

    static Stream<Supplier<AccountRepository>> accountRepositoryProvider() {
        return Stream.of(
                OptimisticAccountRepository::new,
                GlobalLockManagerAccountRepository::new,
                SingleLockAccountRepository::new,
                SynchronizationOnAccountsAccountRepository::new
        );
    }

    @ParameterizedTest
    @MethodSource("accountRepositoryProvider")
    void shouldRegisterAccounts(Supplier<AccountRepository> repository) {
        Bank bank = new Bank(repository.get());
        Account newAccount = new Account("name", randomAmountOfDollars());

        Assertions.assertDoesNotThrow(() -> bank.register(newAccount));
    }

    @ParameterizedTest
    @MethodSource("accountRepositoryProvider")
    void shouldNotRegisterAccountsTwice(Supplier<AccountRepository> repository) {
        Bank bank = new Bank(repository.get());
        Account newAccount = new Account("name", randomAmountOfDollars());

        bank.register(newAccount);

        Assertions.assertThrows(AccountAlreadyRegisteredException.class, () -> bank.register(newAccount));
    }

    @ParameterizedTest
    @MethodSource("accountRepositoryProvider")
    void shouldFetchAccountBalance(Supplier<AccountRepository> repository) {
        Bank bank = new Bank(repository.get());

        Account john = new Account("John", randomAmountOfDollars());
        bank.register(john);

        Assertions.assertEquals(john.balance(), bank.getBalanceFor(john.id()));
    }

    @ParameterizedTest
    @MethodSource("accountRepositoryProvider")
    void shouldThrowExceptionWhenAccountNotFound(Supplier<AccountRepository> repository) {
        Bank bank = new Bank(repository.get());
        Account john = new Account("John", randomAmountOfDollars());
        bank.register(john);

        AccountId unregisteredAccount = new AccountId("Jane");
        Assertions.assertThrows(AccountNotFoundException.class, () -> bank.getBalanceFor(unregisteredAccount));
    }

    @ParameterizedTest
    @MethodSource("accountRepositoryProvider")
    void shouldTransferMoneyBetweenAccounts(Supplier<AccountRepository> repository) {
        Bank bank = new Bank(repository.get());
        Account john = new Account("John", dollars("1000"));
        Account jane = new Account("Jane", dollars("1000"));
        bank.register(john);
        bank.register(jane);

        bank.transfer(john.id(), jane.id(), dollars("500"));

        Assertions.assertEquals(dollars("500"), bank.getBalanceFor(john.id()));
        Assertions.assertEquals(dollars("1500"), bank.getBalanceFor(jane.id()));
    }

    @ParameterizedTest
    @MethodSource("accountRepositoryProvider")
    void shouldFailTransferIfUnknownAccount(Supplier<AccountRepository> repository) {
        Bank bank = new Bank(repository.get());
        Account john = new Account("John", randomAmountOfDollars());
        bank.register(john);

        AccountId unregisteredAccount = new AccountId("Jane");
        Assertions.assertThrows(
                AccountNotFoundException.class,
                () -> bank.transfer(john.id(), unregisteredAccount, dollars("100"))
        );
    }

    @ParameterizedTest
    @MethodSource("accountRepositoryProvider")
    void shouldFailToTransferFromAccountWithInsufficientFunds(Supplier<AccountRepository> repository) {
        Bank bank = new Bank(repository.get());
        Account john = new Account("John", dollars("100"));
        Account jane = new Account("Jane", dollars("100"));
        bank.register(john);
        bank.register(jane);

        Assertions.assertThrows(
                InsufficientFundsException.class,
                () -> bank.transfer(john.id(), jane.id(), dollars("200"))
        );
    }

    @ParameterizedTest
    @MethodSource("accountRepositoryProvider")
    void shouldTransferFromMultipleAccountsAndMultipleBanks(Supplier<AccountRepository> repository) {
        Bank bank = new Bank(repository.get());
        Money initialBalance = dollars("100000");
        Account john = new Account("John", initialBalance);
        Account jane = new Account("Jane", initialBalance);
        Account jack = new Account("Jack", initialBalance);
        Account justin = new Account("Justin", initialBalance);
        bank.register(john);
        bank.register(jane);
        bank.register(jack);

        Bank anotherBank = new Bank(repository.get());
        anotherBank.register(jack);
        anotherBank.register(justin);
        anotherBank.register(john);

        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < initialBalance.amount().intValue(); i++) {

            tasks.add(CompletableFuture.runAsync(() -> bank.transfer(john.id(), jane.id(), dollars("1"))));
            tasks.add(CompletableFuture.runAsync(() -> bank.transfer(jane.id(), jack.id(), dollars("1"))));
            tasks.add(CompletableFuture.runAsync(() -> anotherBank.transfer(jack.id(), justin.id(), dollars("1"))));
            tasks.add(CompletableFuture.runAsync(() -> anotherBank.transfer(justin.id(), john.id(), dollars("1"))));
        }
        tasks.forEach(CompletableFuture::join);

        Assertions.assertEquals(initialBalance, bank.getBalanceFor(john.id()));
        Assertions.assertEquals(initialBalance, bank.getBalanceFor(jane.id()));
        Assertions.assertEquals(initialBalance, bank.getBalanceFor(jack.id()));
        Assertions.assertEquals(initialBalance, anotherBank.getBalanceFor(justin.id()));
    }

    @ParameterizedTest
    @MethodSource("accountRepositoryProvider")
    void shouldTransferFromMultipleAccountWithinASingleBankAndKeepConsistentBalanceAcrossMultipleBanks(Supplier<AccountRepository> repository) {
        Bank bank = new Bank(repository.get());
        Money initialBalance = dollars("100000");
        Account john = new Account("John", initialBalance);
        Account jane = new Account("Jane", initialBalance);
        Account jack = new Account("Jack", initialBalance);
        Account justin = new Account("Justin", initialBalance);
        bank.register(john);
        bank.register(jane);
        bank.register(jack);

        Bank anotherBank = new Bank(repository.get());
        anotherBank.register(jack);
        anotherBank.register(justin);
        anotherBank.register(john);

        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        int halfTheInitialBalance = initialBalance.amount()
                                                  .divide(BigDecimal.valueOf(2), RoundingMode.FLOOR)
                                                  .intValue();
        for (int i = 0; i < halfTheInitialBalance; i++) {
            tasks.add(CompletableFuture.runAsync(() -> bank.transfer(john.id(), jack.id(), dollars("1"))));
            tasks.add(CompletableFuture.runAsync(() -> bank.transfer(john.id(), jane.id(), dollars("1"))));
        }
        tasks.forEach(CompletableFuture::join);
        Assertions.assertEquals(bank.getBalanceFor(john.id()), anotherBank.getBalanceFor(john.id()));
        Assertions.assertEquals(dollars("0"), bank.getBalanceFor(john.id()));
        Assertions.assertEquals(
                initialBalance.amount().add(BigDecimal.valueOf(halfTheInitialBalance)),
                bank.getBalanceFor(jane.id()).amount()
        );
        Assertions.assertEquals(
                initialBalance.amount().add(BigDecimal.valueOf(halfTheInitialBalance)),
                bank.getBalanceFor(jack.id()).amount()
        );
        Assertions.assertEquals(bank.getBalanceFor(jack.id()), anotherBank.getBalanceFor(jack.id()));
    }


    @ParameterizedTest
    @MethodSource("accountRepositoryProvider")
    void shouldLeadToIncorrectFinalBalanceDueToRaceCondition(Supplier<AccountRepository> repository) throws InterruptedException {
        Bank bank = new Bank(repository.get());
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
