package com.dhbw.webeng2.stomo.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Payload for renaming an existing watchlist. */
@Data
public class UpdateWatchlistRequest {
    @NotBlank
    @Size(max = 64, message = "Name must be at most 64 characters")
    private String name;
}
