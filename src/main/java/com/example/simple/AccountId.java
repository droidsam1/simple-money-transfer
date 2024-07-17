package com.example.simple;

public record AccountId(String value) implements Comparable<AccountId> {

    public int compareTo(AccountId id) {
        return this.value.compareToIgnoreCase(id.value);
    }
}
