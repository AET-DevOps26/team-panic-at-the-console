package com.panicattheconsole.userservice.auth;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.openapitools.model.UserRole;

import com.panicattheconsole.userservice.auth.SessionTokenService.SessionUser;
import com.panicattheconsole.userservice.users.UserAccount;

class SessionTokenServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-characters";

    private final SessionTokenService tokens = new SessionTokenService(SECRET, Duration.ofHours(1));

    private UserAccount account() {
        return new UserAccount("alex@example.com", "Alex", "hash", UserRole.MEMBER);
    }

    @Test
    void issueAndParse_roundTripsIdentity() {
        UserAccount account = account();

        Optional<SessionUser> parsed = tokens.parse(tokens.issue(account));

        assertThat(parsed).hasValueSatisfying(session -> {
            assertThat(session.userId()).isEqualTo(account.getId());
            assertThat(session.role()).isEqualTo(UserRole.MEMBER);
        });
    }

    @Test
    void parse_rejectsGarbageAndBlank() {
        assertThat(tokens.parse("not-a-jwt")).isEmpty();
        assertThat(tokens.parse("")).isEmpty();
        assertThat(tokens.parse(null)).isEmpty();
    }

    @Test
    void parse_rejectsTokenSignedWithDifferentSecret() {
        SessionTokenService other = new SessionTokenService(
                "another-secret-that-is-at-least-32-chars!", Duration.ofHours(1));

        assertThat(tokens.parse(other.issue(account()))).isEmpty();
    }

    @Test
    void parse_rejectsExpiredToken() {
        SessionTokenService shortLived = new SessionTokenService(SECRET, Duration.ofSeconds(-5));

        assertThat(tokens.parse(shortLived.issue(account()))).isEmpty();
    }

    @Test
    void constructor_rejectsShortSecret() {
        assertThatThrownBy(() -> new SessionTokenService("too-short", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }
}
