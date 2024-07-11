package com.example.simple;

import java.util.Objects;

public record AccountId(String value) implements Comparable<AccountId> {

    @Override public int compareTo(AccountId o) {
        return Objects.compare(this.value, o.value, String::compareToIgnoreCase);
    }
}
