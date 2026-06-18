package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.model.dto.LoginRequest;
import com.dhbw.webeng2.stomo.model.entity.User;
import com.dhbw.webeng2.stomo.model.enums.Status;
import com.dhbw.webeng2.stomo.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the login brute-force protection in {@link UserService#authenticate}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final int MAX_ATTEMPTS = 3;

    @Mock
    private UserRepo userRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepo, passwordEncoder, MAX_ATTEMPTS, 15);
    }

    @Test
    void wrongPasswordIncrementsFailedAttempts() {
        User user = userWith(0, null, Status.ACTIVE);
        when(userRepo.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.authenticate(login("bad")))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
        verify(userRepo).save(user);
    }

    @Test
    void locksAccountAfterMaxAttempts() {
        User user = userWith(MAX_ATTEMPTS - 1, null, Status.ACTIVE);
        when(userRepo.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.authenticate(login("bad")))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(user.getLockedUntil()).isNotNull();
        assertThat(user.getStatus()).isEqualTo(Status.LOCKED);
    }

    @Test
    void rejectsLoginWhileLocked() {
        User user = userWith(MAX_ATTEMPTS, Instant.now().plus(10, ChronoUnit.MINUTES), Status.LOCKED);
        when(userRepo.findByEmail("a@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.authenticate(login("whatever")))
                .isInstanceOf(LockedException.class);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void successResetsFailedAttempts() {
        User user = userWith(2, null, Status.ACTIVE);
        when(userRepo.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good", "hash")).thenReturn(true);

        User result = service.authenticate(login("good"));

        assertThat(result).isSameAs(user);
        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(userRepo).save(user);
    }

    @Test
    void expiredLockIsClearedOnSuccessfulLogin() {
        User user = userWith(MAX_ATTEMPTS, Instant.now().minus(1, ChronoUnit.MINUTES), Status.LOCKED);
        when(userRepo.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good", "hash")).thenReturn(true);

        service.authenticate(login("good"));

        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);
        verify(userRepo).save(user);
    }

    private static User userWith(int attempts, Instant lockedUntil, Status status) {
        User user = new User();
        user.setId(1L);
        user.setEmail("a@example.com");
        user.setPassword("hash");
        user.setStatus(status);
        user.setFailedLoginAttempts(attempts);
        user.setLockedUntil(lockedUntil);
        return user;
    }

    private static LoginRequest login(String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail("a@example.com");
        req.setPassword(password);
        return req;
    }
}
