package com.panicattheconsole.userservice.users;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.openapitools.model.UserRole;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.panicattheconsole.userservice.exception.EmailAlreadyRegisteredException;
import com.panicattheconsole.userservice.exception.InvalidCredentialsException;
import com.panicattheconsole.userservice.exception.InvalidProfileUpdateException;
import com.panicattheconsole.userservice.exception.NotAuthenticatedException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Account registration, credential checks, and the user directory.
 * Passwords are stored as BCrypt hashes; emails are normalized to lowercase.
 */
@Service
@Transactional
public class UserAccountService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PersistenceContext
    private EntityManager entityManager;

    public UserAccountService(UserAccountRepository repository) {
        this.repository = repository;
    }

    public UserAccount register(String email, String password, String displayName) {
        String normalizedEmail = normalize(email);
        if (repository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }
        UserAccount account = new UserAccount(
                normalizedEmail, displayName, passwordEncoder.encode(password), UserRole.MEMBER);
        return repository.save(account);
    }

    @Transactional(readOnly = true)
    public UserAccount authenticate(String email, String password) {
        UserAccount account = repository.findByEmail(normalize(email)).orElse(null);
        if (account == null || !passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return account;
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findById(UUID id) {
        return repository.findById(id);
    }

    /**
     * Updates the caller's own profile. Changing the email requires the current
     * password so a hijacked session cannot silently take over the account.
     */
    public UserAccount updateProfile(UUID id, String email, String displayName, String currentPassword) {
        if (email == null && displayName == null) {
            throw new InvalidProfileUpdateException("Provide at least one of email or displayName");
        }
        UserAccount account = repository.findById(id).orElseThrow(NotAuthenticatedException::new);
        if (email != null) {
            String normalizedEmail = normalize(email);
            if (!normalizedEmail.equals(account.getEmail())) {
                if (currentPassword == null || currentPassword.isBlank()) {
                    throw new InvalidProfileUpdateException("Changing the email requires currentPassword");
                }
                if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
                    throw new InvalidCredentialsException();
                }
                if (repository.existsByEmail(normalizedEmail)) {
                    throw new EmailAlreadyRegisteredException(normalizedEmail);
                }
                account.setEmail(normalizedEmail);
            }
        }
        if (displayName != null) {
            account.setDisplayName(displayName);
        }
        return repository.save(account);
    }

    public void changePassword(UUID id, String currentPassword, String newPassword) {
        UserAccount account = repository.findById(id).orElseThrow(NotAuthenticatedException::new);
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        repository.save(account);
    }

    @Transactional(readOnly = true)
    public List<UserAccount> list(int limit, int offset) {
        return entityManager
                .createQuery("select u from UserAccount u order by u.createdAt asc, u.id asc", UserAccount.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
