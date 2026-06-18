package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.exception.EmailAlreadyExistsException;
import com.dhbw.webeng2.stomo.model.dto.LoginRequest;
import com.dhbw.webeng2.stomo.model.dto.RegisterRequest;
import com.dhbw.webeng2.stomo.model.entity.User;
import com.dhbw.webeng2.stomo.model.enums.Status;
import com.dhbw.webeng2.stomo.repository.UserRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final int maxLoginAttempts;
    private final long lockoutMinutes;

    public UserService(UserRepo userRepo,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.security.max-login-attempts:5}") int maxLoginAttempts,
                       @Value("${app.security.lockout-minutes:15}") long lockoutMinutes) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.maxLoginAttempts = maxLoginAttempts;
        this.lockoutMinutes = lockoutMinutes;
    }

    /** Create a new user. Password is BCrypt-hashed; never stored in clear. */
    public User register(RegisterRequest dto) {
        if (userRepo.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("E-Mail existiert schon");
        }

        User user = new User();
        user.setFirstname(dto.getFirstname());
        user.setLastname(dto.getLastname());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(Status.ACTIVE);
        return userRepo.save(user);
    }

    /**
     * Verify credentials with brute-force protection: after {@code maxLoginAttempts}
     * consecutive failures the account is locked for {@code lockoutMinutes}. Throws
     * {@link BadCredentialsException} (mapped to 401) on a mismatch and
     * {@link LockedException} (mapped to 423) while locked.
     */
    public User authenticate(LoginRequest dto) {
        User user = userRepo.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        boolean dirty = false;
        if (user.getLockedUntil() != null) {
            if (user.getLockedUntil().isAfter(Instant.now())) {
                throw new LockedException("Account temporarily locked due to repeated failed logins.");
            }
            clearLock(user); // lock period served — start fresh
            dirty = true;
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            registerFailure(user); // persists the incremented (and possibly locked) state
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            dirty = true;
        }
        if (dirty) {
            userRepo.save(user);
        }
        return user;
    }

    private void registerFailure(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= maxLoginAttempts) {
            user.setLockedUntil(Instant.now().plus(lockoutMinutes, ChronoUnit.MINUTES));
            user.setStatus(Status.LOCKED);
        }
        userRepo.save(user);
    }

    private void clearLock(User user) {
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        if (user.getStatus() == Status.LOCKED) {
            user.setStatus(Status.ACTIVE);
        }
    }
}
