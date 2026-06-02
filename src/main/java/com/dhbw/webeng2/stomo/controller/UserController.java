package com.dhbw.webeng2.stomo.controller;

import com.dhbw.webeng2.stomo.model.dto.UserDto;
import com.dhbw.webeng2.stomo.model.enums.MessageType;
import com.dhbw.webeng2.stomo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Log4j2
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserDto userDto) {
        log.info("{}: {}: {}: {}",
                MessageType.INFO,
                OffsetDateTime.now(),
                "Request for Registration: ",
                userDto.getFirstname()
        );

        userService.registerUser(userDto);

        log.info("{}: {}: {}: {}",
                MessageType.INFO,
                OffsetDateTime.now(),
                "Successfull registration: ",
                userDto.getFirstname()
        );

        return new ResponseEntity<>("Benutzer erfolgreich erstellt.", HttpStatus.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        return new ResponseEntity<>(HttpStatus.valueOf(getCurrentUser().getBody().getEmail()));
    }
}
