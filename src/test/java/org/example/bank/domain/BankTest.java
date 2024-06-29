package org.example.bank.domain;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.example.bank.domain.account.Account;
import org.example.bank.domain.account.AccountId;
import org.example.bank.domain.account.MutableAccount;
import org.example.bank.domain.exceptions.AccountNotFoundException;
import org.example.bank.domain.exceptions.InsufficientFundsException;
import org.example.bank.domain.exceptions.NegativeTransferAmountException;
import org.example.bank.domain.money.Money;
import org.example.bank.domain.strategy.OptimisticLockTransferStrategy;
import org.example.bank.domain.strategy.PessimisticLockTransferStrategy;
import org.example.bank.domain.strategy.SerializedTransferStrategy;
import org.example.bank.domain.strategy.TransferStrategy;
import org.example.bank.infraestructure.account.repository.inmemory.InMemoryAccountRepository;
import org.example.bank.infraestructure.account.repository.inmemory.InMemoryConcurrentDataStructureAccountRepository;
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

        var currentBalance = this.bank.getBalance(new AccountId("A"));

        Assertions.assertEquals(initialBalance, currentBalance);
    }

    @Test
    void shouldThrowExceptionWhenAccountNotFound() {
        var unregisteredAccount = new AccountId("A");
        Assertions.assertThrows(AccountNotFoundException.class, () -> this.bank.getBalance(unregisteredAccount));
    }

    @ParameterizedTest
    @ArgumentsSource(TransferStrategyProvider.class)
    void shouldTransferMoney(TransferStrategy strategy) {
        var a = createAccountWithBalance("A", euros(100));
        var b = createAccountWithBalance("B", euros(100));
        this.bank = new Bank(new InMemoryAccountRepository(strategy));
        this.bank.registerAccount(a);
        this.bank.registerAccount(b);

        bank.transfer(euros(50), a.id(), b.id());

        Assertions.assertEquals(euros(50), this.bank.getBalance(a.id()));
        Assertions.assertEquals(euros(150), this.bank.getBalance(b.id()));
    }

    @ParameterizedTest
    @ArgumentsSource(TransferStrategyProvider.class)
    void shouldSupportConcurrentlyTransfers(TransferStrategy strategy) {
        var originalBalance = euros(20_000);
        var a = createAccountWithBalance("A", originalBalance);
        var b = createAccountWithBalance("B", originalBalance);
        this.bank = new Bank(new InMemoryAccountRepository(strategy));
        this.bank.registerAccount(a);
        this.bank.registerAccount(b);

        int numberOfTransfers = 10_000;
        var futures = new ArrayList<CompletableFuture<Void>>();
        for (int i = 0; i < numberOfTransfers; i++) {
            futures.add(CompletableFuture.runAsync(() -> bank.transfer(euros(1), a.id(), b.id())));
        }
        futures.forEach(CompletableFuture::join);

        Assertions.assertEquals(originalBalance.subtract(numberOfTransfers), this.bank.getBalance(a.id()));
        Assertions.assertEquals(originalBalance.add(numberOfTransfers), this.bank.getBalance(b.id()));
    }

    @ParameterizedTest
    @ArgumentsSource(TransferStrategyProvider.class)
    void shouldSupportConcurrentlyTransfersWhenMoreThanTwoAccountsAreMakingConcurrentTransfers(TransferStrategy strategy) {
        var originalBalance = euros(20_000);
        var a = createAccountWithBalance("A", originalBalance);
        var b = createAccountWithBalance("B", originalBalance);
        var c = createAccountWithBalance("AA", originalBalance);
        this.bank = new Bank(new InMemoryAccountRepository(strategy));

        this.bank.registerAccount(a);
        this.bank.registerAccount(b);
        this.bank.registerAccount(c);

        int numberOfTransfers = 10_000;
        // move money from A to B
        var futures = new ArrayList<CompletableFuture<Void>>();
        for (int i = 0; i < numberOfTransfers; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                bank.transfer(euros(1), a.id(), b.id());
                bank.transfer(euros(1), c.id(), a.id());
            }));
        }
        // move money from B to A
        for (int i = 0; i < numberOfTransfers; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                bank.transfer(euros(1), b.id(), a.id());
                bank.transfer(euros(1), a.id(), c.id());
            }));
        }
        futures.forEach(CompletableFuture::join);

        Assertions.assertEquals(this.bank.getBalance(a.id()), this.bank.getBalance(b.id()));
        Assertions.assertEquals(originalBalance, this.bank.getBalance(a.id()));
        Assertions.assertEquals(originalBalance, this.bank.getBalance(c.id()));
    }

    @ParameterizedTest
    @ArgumentsSource(TransferStrategyProvider.class)
    void shouldNotAllowTransfersWithNegativeAmounts(TransferStrategy strategy) {
        this.bank = new Bank(new InMemoryAccountRepository(strategy));
        var accountA = createAccountWithBalance("A", euros(100));
        this.bank.registerAccount(accountA);
        var accountB = createAccountWithBalance("B", euros(100));
        this.bank.registerAccount(accountB);
        var negativeAmount = euros(-1);

        Assertions.assertThrows(
                NegativeTransferAmountException.class,
                () -> this.bank.transfer(negativeAmount, accountA.id(), accountB.id())
        );
    }

    @ParameterizedTest
    @ArgumentsSource(TransferStrategyProvider.class)
    void shouldNotAllowTransfersWhenDepositorHasInsufficientFunds(TransferStrategy strategy) {
        this.bank = new Bank(new InMemoryAccountRepository(strategy));
        var accountA = createAccountWithBalance("A", euros(0));
        this.bank.registerAccount(accountA);
        var accountB = createAccountWithBalance("B", euros(0));
        this.bank.registerAccount(accountB);
        var negativeAmount = euros(1);

        Assertions.assertThrows(
                InsufficientFundsException.class,
                () -> this.bank.transfer(negativeAmount, accountA.id(), accountB.id())
        );
    }

    @RepeatedTest(10)
    void compareTransferMethodsEfficiency(RepetitionInfo repetitionInfo) {
        var a = createAccountWithBalance("A", euros(1000));
        var b = createAccountWithBalance("B", euros(1000));
        this.bank.registerAccount(a);
        this.bank.registerAccount(b);

        int numberOfTransfers = 1000;

        long totalConcurrentHashMap = 0;
        long totalSerializedTransferTime = 0;
        long totalPessimisticLockTransferTime = 0;
        long totalOptimisticLockTransferTime = 0;

        // Measure serializedTransfer
        long start = System.nanoTime();
        for (int i = 0; i < numberOfTransfers; i++) {
            concurrentlyTransfer(new Bank(new InMemoryConcurrentDataStructureAccountRepository()), numberOfTransfers);
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

    private Account createAccountWithBalance(String id, Money balance) {
        return new MutableAccount(id, balance);
    }


    private static Money euros(int amount) {
        return new Money(amount, "EUR");
    }

    private static Money randomMoney() {
        return new Money(ThreadLocalRandom.current().nextInt(0, 1000), "EUR");
    }


    static class TransferStrategyProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(new SerializedTransferStrategy()),
                    Arguments.of(new OptimisticLockTransferStrategy()),
                    Arguments.of(new PessimisticLockTransferStrategy())
            );
        }
    }

}
