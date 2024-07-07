package org.example.bank.domain.account;

import java.util.Objects;
import java.util.function.IntSupplier;

public record MalfunctioningHashcodeAccountId(String value, IntSupplier hasCodeSupplier) implements AccountId {

    public MalfunctioningHashcodeAccountId(String value) {
        this(value, () -> 0);
    }

    @Override public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        MalfunctioningHashcodeAccountId accountId = (MalfunctioningHashcodeAccountId) object;
        return Objects.equals(value, accountId.value);
    }

    @Override public int hashCode() {
        return hasCodeSupplier.getAsInt();
    }
}
