package com.dhbw.webeng2.stomo.model.dto;

import com.dhbw.webeng2.stomo.model.entity.User;
import lombok.Builder;
import lombok.Data;

/** Public view of a user — never carries the password hash. */
@Data
@Builder
public class UserResponse {
    private Long id;
    private String firstname;
    private String lastname;
    private String email;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .build();
    }
}
