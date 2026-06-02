package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.exception.EmailAlreadyExistsException;
import com.dhbw.webeng2.stomo.model.dto.UserDto;
import com.dhbw.webeng2.stomo.model.entity.User;
import com.dhbw.webeng2.stomo.repository.UserRepo;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public void registerUser(UserDto dto) {
        if (userRepo.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("E-Mail existiert schon");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        userRepo.save(user);
    }
}
