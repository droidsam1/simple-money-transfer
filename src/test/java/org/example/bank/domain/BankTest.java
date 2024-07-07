package org.example.bank.domain;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.MalfunctioningHashcodeAccountId;
import org.example.bank.domain.account.ReadWriteLockAccount;
import org.example.bank.domain.account.SimpleAccountId;
import org.example.bank.domain.exceptions.AccountNotFoundException;
import org.example.bank.domain.exceptions.InsufficientFundsException;
import org.example.bank.domain.exceptions.NegativeTransferAmountException;
import org.example.bank.domain.money.Money;
import org.example.bank.infraestructure.account.repository.inmemory.InMemoryAccountRepository;
import org.example.bank.infraestructure.account.repository.inmemory.InMemoryAccountRepositoryConcurrentHashMap;
import org.example.bank.infraestructure.account.repository.inmemory.strategy.OptimisticLockTransferStrategy;
import org.example.bank.infraestructure.account.repository.inmemory.strategy.PessimisticLockTransferStrategy;
import org.example.bank.infraestructure.account.repository.inmemory.strategy.RevisitedPessimisticLockTransferStrategy;
import org.example.bank.infraestructure.account.repository.inmemory.strategy.SerializedTransferStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

class BankTest {

    //SUT
    private Bank bank;

    @BeforeEach
    public void setUp() {
        bank = new Bank();
    }

    @Test
    void shouldRegisterAccount() {
        Assertions.assertDoesNotThrow(() -> bank.registerAccount(createAccountWithBalance("A", euros(0))));
    }

    @Test
    void shouldRetrieveTheBalanceOfAnAccount() {
        var initialBalance = randomMoney();
        this.bank.registerAccount(createAccountWithBalance("A", initialBalance));

        var currentBalance = this.bank.getBalance(new SimpleAccountId("A"));

        Assertions.assertEquals(initialBalance, currentBalance);
    }

    @Test
    void shouldThrowExceptionWhenAccountNotFound() {
        var unregisteredAccount = new SimpleAccountId("A");
        Assertions.assertThrows(AccountNotFoundException.class, () -> this.bank.getBalance(unregisteredAccount));
    }

    @ParameterizedTest
    @ArgumentsSource(BankProvider.class)
    void shouldTransferMoney(Bank bankInstance) {
        var a = createAccountWithBalance("A", euros(100));
        var b = createAccountWithBalance("B", euros(100));
        bankInstance.registerAccount(a);
        bankInstance.registerAccount(b);

        bankInstance.transfer(euros(50), a.id(), b.id());

        Assertions.assertEquals(euros(50), bankInstance.getBalance(a.id()));
        Assertions.assertEquals(euros(150), bankInstance.getBalance(b.id()));
    }

    @ParameterizedTest
    @ArgumentsSource(BankProvider.class)
    void shouldSupportConcurrentlyTransfers(Bank bankInstance) {
        var originalBalance = euros(20_000);
        var a = createAccountWithBalance("A", originalBalance);
        var b = createAccountWithBalance("B", originalBalance);
        bankInstance.registerAccount(a);
        bankInstance.registerAccount(b);

        int numberOfTransfers = 10_000;
        var futures = new ArrayList<CompletableFuture<Void>>();
        for (int i = 0; i < numberOfTransfers; i++) {
            futures.add(CompletableFuture.runAsync(() -> bankInstance.transfer(euros(1), a.id(), b.id())));
        }
        futures.forEach(CompletableFuture::join);

        Assertions.assertEquals(originalBalance.subtract(numberOfTransfers), bankInstance.getBalance(a.id()));
        Assertions.assertEquals(originalBalance.add(numberOfTransfers), bankInstance.getBalance(b.id()));
    }

    @ParameterizedTest
    @ArgumentsSource(BankProvider.class)
    void shouldSupportConcurrentlyTransfersWhenMoreThanTwoAccountsAreMakingConcurrentTransfers(Bank bankInstance) {
        var originalBalance = euros(20_000);
        var a = createAccountWithBalance("A", originalBalance);
        var b = createAccountWithBalance("B", originalBalance);
        var c = createAccountWithBalance("AA", originalBalance);

        bankInstance.registerAccount(a);
        bankInstance.registerAccount(b);
        bankInstance.registerAccount(c);

        int numberOfTransfers = 10_000;
        // move money from A to B
        var futures = new ArrayList<CompletableFuture<Void>>();
        for (int i = 0; i < numberOfTransfers; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                bankInstance.transfer(euros(1), a.id(), b.id());
                bankInstance.transfer(euros(1), c.id(), a.id());
            }));
        }
        // move money from B to A
        for (int i = 0; i < numberOfTransfers; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                bankInstance.transfer(euros(1), b.id(), a.id());
                bankInstance.transfer(euros(1), a.id(), c.id());
            }));
        }
        futures.forEach(CompletableFuture::join);

        Assertions.assertEquals(bankInstance.getBalance(a.id()), bankInstance.getBalance(b.id()));
        Assertions.assertEquals(originalBalance, bankInstance.getBalance(a.id()));
        Assertions.assertEquals(originalBalance, bankInstance.getBalance(c.id()));
    }

    @ParameterizedTest
    @ArgumentsSource(BankProvider.class)
    void shouldNotAllowTransfersWithNegativeAmounts(Bank bankInstance) {
        var accountA = createAccountWithBalance("A", euros(100));
        bankInstance.registerAccount(accountA);
        var accountB = createAccountWithBalance("B", euros(100));
        bankInstance.registerAccount(accountB);
        var negativeAmount = euros(-1);

        Assertions.assertThrows(
                NegativeTransferAmountException.class,
                () -> bankInstance.transfer(negativeAmount, accountA.id(), accountB.id())
        );
    }

    @ParameterizedTest
    @ArgumentsSource(BankProvider.class)
    void shouldNotAllowTransfersWhenDepositorHasInsufficientFunds(Bank bankInstance) {
        var accountA = createAccountWithBalance("A", euros(0));
        bankInstance.registerAccount(accountA);
        var accountB = createAccountWithBalance("B", euros(0));
        bankInstance.registerAccount(accountB);
        var negativeAmount = euros(1);

        Assertions.assertThrows(
                InsufficientFundsException.class,
                () -> bankInstance.transfer(negativeAmount, accountA.id(), accountB.id())
        );
    }

    @RepeatedTest(3)
    void compareTransferMethodsEfficiency(RepetitionInfo repetitionInfo) {
        var a = createAccountWithBalance("A", euros(1000));
        var b = createAccountWithBalance("B", euros(1000));
        this.bank.registerAccount(a);
        this.bank.registerAccount(b);

        int numberOfTransfers = 1_000;

        long totalConcurrentHashMap = 0;
        long totalSerializedTransferTime = 0;
        long totalPessimisticLockTransferTime = 0;
        long totalOptimisticLockTransferTime = 0;

        // Measure serializedTransfer
        long start = System.nanoTime();
        for (int i = 0; i < numberOfTransfers; i++) {
            concurrentlyTransfer(new Bank(new InMemoryAccountRepositoryConcurrentHashMap()), numberOfTransfers);
        }
        long end = System.nanoTime();
        long concurrentHashMapTransferTime = end - start;
        totalConcurrentHashMap += concurrentHashMapTransferTime;

        // Measure serializedTransfer
        start = System.nanoTime();
        for (int i = 0; i < numberOfTransfers; i++) {
            concurrentlyTransfer(
                    new Bank(new InMemoryAccountRepository(new SerializedTransferStrategy())),
                    numberOfTransfers
            );
        }
        end = System.nanoTime();
        long serializedTransferTime = end - start;
        totalSerializedTransferTime += serializedTransferTime;

        // Measure pessimisticLockTransfer
        start = System.nanoTime();
        for (int i = 0; i < numberOfTransfers; i++) {
            concurrentlyTransfer(
                    new Bank(new InMemoryAccountRepository(new PessimisticLockTransferStrategy())),
                    numberOfTransfers
            );
        }
        end = System.nanoTime();
        long pessimisticLockTransferTime = end - start;
        totalPessimisticLockTransferTime += pessimisticLockTransferTime;

        // Measure optimisticLockTransfer
        start = System.nanoTime();
        for (int i = 0; i < numberOfTransfers; i++) {
            concurrentlyTransfer(
                    new Bank(new InMemoryAccountRepository(new OptimisticLockTransferStrategy())),
                    numberOfTransfers
            );
        }
        end = System.nanoTime();
        long optimisticLockTransferTime = end - start;
        totalOptimisticLockTransferTime += optimisticLockTransferTime;

        // Print the mean time taken and speedup after all repetitions
        if (repetitionInfo.getCurrentRepetition() == repetitionInfo.getTotalRepetitions()) {
            System.out.println("Mean time taken by concurrentHashMap: " + (totalConcurrentHashMap / repetitionInfo.getTotalRepetitions()) + " ns");
            System.out.println("Mean time taken by serializedTransfer: " + (totalSerializedTransferTime / repetitionInfo.getTotalRepetitions()) + " ns");
            System.out.println("Mean time taken by pessimisticLockTransfer: " + (totalPessimisticLockTransferTime / repetitionInfo.getTotalRepetitions()) + " ns");
            System.out.println("Mean time taken by optimisticLockTransfer: " + (totalOptimisticLockTransferTime / repetitionInfo.getTotalRepetitions()) + " ns");
            System.out.println("---------------------------------------------");
            System.out.printf(
                    "Mean speedup of serializedTransfer: %.2f x%n",
                    (double) totalSerializedTransferTime / totalSerializedTransferTime
            );
            System.out.printf(
                    "Mean speedup of concurrentHashMap: %.2f x%n",
                    (double) totalSerializedTransferTime / totalConcurrentHashMap
            );
            System.out.printf(
                    "Mean speedup of pessimisticLockTransfer: %.2f x%n",
                    (double) totalSerializedTransferTime / totalPessimisticLockTransferTime
            );
            System.out.printf(
                    "Mean speedup of optimisticLockTransfer: %.2f x %n",
                    (double) totalSerializedTransferTime / totalOptimisticLockTransferTime
            );
        }
    }

    void concurrentlyTransfer(Bank bankInstance, int numberOfTransfers) {
        var originalBalance = euros(numberOfTransfers);
        var a = createAccountWithBalance("A", originalBalance);
        var b = createAccountWithBalance("B", originalBalance);
        var c = createAccountWithBalance("AA", originalBalance);
        var d = createAccountWithBalance("BB", originalBalance);

        bankInstance.registerAccount(a);
        bankInstance.registerAccount(b);
        bankInstance.registerAccount(c);
        bankInstance.registerAccount(d);

        // move money from A to B
        var futures = new ArrayList<CompletableFuture<Void>>();
        for (int i = 0; i < numberOfTransfers; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                bankInstance.transfer(euros(1), a.id(), b.id());
                bankInstance.transfer(euros(1), c.id(), a.id());
                bankInstance.transfer(euros(1), d.id(), c.id());
            }));
        }
        // move money from B to A
        for (int i = 0; i < numberOfTransfers; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                bankInstance.transfer(euros(1), b.id(), a.id());
                bankInstance.transfer(euros(1), a.id(), c.id());
                bankInstance.transfer(euros(1), c.id(), d.id());
            }));
        }
        futures.forEach(CompletableFuture::join);
        Assertions.assertEquals(bankInstance.getBalance(a.id()), bankInstance.getBalance(b.id()));
        Assertions.assertEquals(originalBalance, bankInstance.getBalance(a.id()));
        Assertions.assertEquals(originalBalance, bankInstance.getBalance(c.id()));
    }

    @ParameterizedTest
    @ArgumentsSource(BankProvider.class)
    void shouldTransferMoneyWhenLockOrderingFailsToDetermineWithAccountToLockFirst(Bank bankInstance) {
        var originalBalance = euros(20_000);
        var a = createMalfunctioningAccountWithBalance("A", originalBalance);
        var b = createMalfunctioningAccountWithBalance("B", originalBalance);
        var c = createMalfunctioningAccountWithBalance("AA", originalBalance);

        bankInstance.registerAccount(a);
        bankInstance.registerAccount(b);
        bankInstance.registerAccount(c);

        int numberOfTransfers = 10_000;
        // move money from A to B
        var futures = new ArrayList<CompletableFuture<Void>>();
        for (int i = 0; i < numberOfTransfers; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                bankInstance.transfer(euros(1), a.id(), b.id());
                bankInstance.transfer(euros(1), c.id(), a.id());
            }));
        }
        // move money from B to A
        for (int i = 0; i < numberOfTransfers; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                bankInstance.transfer(euros(1), b.id(), a.id());
                bankInstance.transfer(euros(1), a.id(), c.id());
            }));
        }
        futures.forEach(CompletableFuture::join);

        Assertions.assertEquals(bankInstance.getBalance(a.id()), bankInstance.getBalance(b.id()));
        Assertions.assertEquals(originalBalance, bankInstance.getBalance(a.id()));
        Assertions.assertEquals(originalBalance, bankInstance.getBalance(c.id()));
    }

    private Account createAccountWithBalance(String id, Money balance) {
        return new ReadWriteLockAccount(id, balance);
    }

    private Account createMalfunctioningAccountWithBalance(String id, Money balance) {
        return new ReadWriteLockAccount(new MalfunctioningHashcodeAccountId(id), balance);
    }


    private static Money euros(int amount) {
        return new Money(amount, "EUR");
    }

    private static Money randomMoney() {
        return new Money(ThreadLocalRandom.current().nextInt(0, 1000), "EUR");
    }

    static class BankProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(new Bank(new InMemoryAccountRepository(new OptimisticLockTransferStrategy()))),
                    Arguments.of(new Bank(new InMemoryAccountRepository(new RevisitedPessimisticLockTransferStrategy()))),
                    Arguments.of(new Bank(new InMemoryAccountRepository(new SerializedTransferStrategy()))),
                    Arguments.of(new Bank(new InMemoryAccountRepositoryConcurrentHashMap()))
            );
        }
    }

}
