package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.exception.EmailAlreadyExistsException;
import com.dhbw.webeng2.stomo.model.dto.LoginRequest;
import com.dhbw.webeng2.stomo.model.dto.RegisterRequest;
import com.dhbw.webeng2.stomo.model.entity.User;
import com.dhbw.webeng2.stomo.model.enums.Status;
import com.dhbw.webeng2.stomo.repository.UserRepo;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
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

    /** Verify credentials. Throws {@link BadCredentialsException} (mapped to 401) on any mismatch. */
    public User authenticate(LoginRequest dto) {
        User user = userRepo.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return user;
    }
}
