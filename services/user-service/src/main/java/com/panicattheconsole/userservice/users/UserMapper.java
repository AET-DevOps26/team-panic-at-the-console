package com.panicattheconsole.userservice.users;

import java.time.ZoneOffset;

import org.openapitools.model.User;

/** Maps the persistence entity to the generated API model (never the hash). */
public final class UserMapper {

    private UserMapper() {}

    public static User toApi(UserAccount account) {
        return new User(
                account.getId(),
                account.getEmail(),
                account.getDisplayName(),
                account.getRole(),
                account.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}
