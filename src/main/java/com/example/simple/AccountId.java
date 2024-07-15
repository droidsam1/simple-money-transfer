package com.example.simple;

public record AccountId(String value) implements Comparable<AccountId> {

    @Override public int compareTo(AccountId o) {
        return this.value().compareToIgnoreCase(o.value());
    }
}
