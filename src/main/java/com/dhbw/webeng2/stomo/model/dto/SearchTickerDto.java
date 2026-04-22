package com.dhbw.webeng2.stomo.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchTickerDto {
    private String symbol;
    private String name;
    private String type;
    private String region;

    @JsonProperty("bestMatches")
    private List<SearchTickerDto> matches;
}
