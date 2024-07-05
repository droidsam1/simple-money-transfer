package org.example.bank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

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

    private Money dollars(String amount) {
        return new Money(amount, "USD");
    }


}