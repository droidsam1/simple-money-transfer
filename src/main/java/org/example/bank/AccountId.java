package org.example.bank;

import java.util.UUID;

public record AccountId(UUID value) {

    public AccountId() {
        this(UUID.randomUUID());
    }

}
