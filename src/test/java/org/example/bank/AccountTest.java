package org.example.bank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class AccountTest {


    @RepeatedTest(1000)
    void shouldConcurrentlyTransfer() {
        Account client1 = new Account("client 1", dollars("1000"));
        Account client2 = new Account("client 2", dollars("1000"));
        Account client3 = new Account("client 2", dollars("1000"));

        Collection<CompletableFuture<Void>> transfers = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            transfers.add(CompletableFuture.runAsync(() -> client1.withdraw(dollars("1"))));
            transfers.add(CompletableFuture.runAsync(() -> client2.deposit(dollars("1"))));

            transfers.add(CompletableFuture.runAsync(() -> client2.withdraw(dollars("1"))));
            transfers.add(CompletableFuture.runAsync(() -> client3.deposit(dollars("1"))));

            transfers.add(CompletableFuture.runAsync(() -> client3.withdraw(dollars("1"))));
            transfers.add(CompletableFuture.runAsync(() -> client1.deposit(dollars("1"))));
        }
        transfers.forEach(CompletableFuture::join);

        Assertions.assertEquals(dollars("1000"), client1.balance());
        Assertions.assertEquals(dollars("1000"), client2.balance());
        Assertions.assertEquals(dollars("1000"), client3.balance());
    }

    @Test
    void shouldConcurrentlyTransferWithIntermediateMethod() {
        Account client1 = new Account("client 1", dollars("1000"));
        Account client2 = new Account("client 2", dollars("1000"));
        Account client3 = new Account("client 2", dollars("1000"));

        Collection<CompletableFuture<Void>> transfers = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            transfers.add(CompletableFuture.runAsync(() -> transfer(client1, client2, dollars("1"))));

            transfers.add(CompletableFuture.runAsync(() -> transfer(client2, client3, dollars("1"))));

            transfers.add(CompletableFuture.runAsync(() -> transfer(client3, client1, dollars("1"))));

        }
        // Try to force a  deadlock
        CompletableFuture.runAsync(() -> tryToIntroduceDeadlock(client2));

        transfers.forEach(CompletableFuture::join);

        Assertions.assertEquals(dollars("1000"), client1.balance());
        Assertions.assertEquals(dollars("1000"), client2.balance());
        Assertions.assertEquals(dollars("1000"), client3.balance());
    }

    private static void tryToIntroduceDeadlock(Account client2) {
        try {
            synchronized (client2) {
                while (true) {
                    Thread.sleep(Integer.MAX_VALUE);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private Money dollars(String amount) {
        return new Money(amount, "USD");
    }


    private void transfer(Account from, Account to, Money amount) {
        //                pessimisticTransfer(from, to, amount);
        optimisticTransfer(from, to, amount);
    }

    private static void optimisticTransfer(Account from, Account to, Money amount) {
        while (!from.compareAndWithdraw(from.balance(), amount)) {
        }
        while (!to.compareAndDeposit(to.balance(), amount)) {
        }
    }


    private static void pessimisticTransfer(Account from, Account to, Money amount) {
        if (from.id().toString().compareToIgnoreCase(to.id().toString()) < 0) {
            synchronized (from) {
                synchronized (to) {
                    from.withdraw(amount);
                    to.deposit(amount);
                }
            }
        } else {
            synchronized (to) {
                synchronized (from) {
                    from.withdraw(amount);
                    to.deposit(amount);
                }
            }
        }
    }


}