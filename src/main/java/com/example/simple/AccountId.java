package com.example.simple;

public record AccountId(String value) implements Comparable<AccountId> {

    @Override
    public int compareTo(AccountId id) {
        return id.value().compareTo(this.value());
    }
}
