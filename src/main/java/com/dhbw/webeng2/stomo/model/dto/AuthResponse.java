package com.dhbw.webeng2.stomo.model.dto;

import lombok.Builder;
import lombok.Data;

/** Returned by register/login: the bearer token plus the (password-free) user. */
@Data
@Builder
public class AuthResponse {
    private String token;
    private UserResponse user;
}
