package org.example.bank.domain.account;

import org.example.bank.domain.money.Money;

public interface Account {

    Account withdraw(Money amount);

    Account deposit(Money amount);

    Money balance();

    AccountId id();

    void compareAndSetBalance(Money originBalance, Money newValue);
}
