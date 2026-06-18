package com.dhbw.webeng2.stomo.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Payload for adding a symbol to a watchlist. */
@Data
public class AddSymbolRequest {
    @NotBlank
    @Size(max = 32, message = "Symbol must be at most 32 characters")
    private String symbol;
}
