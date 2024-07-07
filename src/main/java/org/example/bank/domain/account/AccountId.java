package org.example.bank.domain.account;

import java.util.Objects;
import java.util.function.IntSupplier;

public record AccountId(String value, IntSupplier hasCodeSupplier) {

    public AccountId(String value) {
        this(value, () -> Objects.hashCode(value));
    }

    @Override public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        AccountId accountId = (AccountId) object;
        return Objects.equals(value, accountId.value);
    }

    @Override public int hashCode() {
        return hasCodeSupplier.getAsInt();
    }
}
