package com.dhbw.webeng2.stomo.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Payload for creating a new watchlist. */
@Data
public class CreateWatchlistRequest {
    @NotBlank
    @Size(max = 64, message = "Name must be at most 64 characters")
    private String name;
}
