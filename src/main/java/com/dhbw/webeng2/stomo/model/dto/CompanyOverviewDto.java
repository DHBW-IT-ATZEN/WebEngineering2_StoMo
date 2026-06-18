package com.dhbw.webeng2.stomo.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyOverviewDto {
    private String symbol;
    private String name;
    private String exchange;
    private String sector;
    private String description;
    private Long marketCap;
    private Double peRatio;
    private Double dividendYield;
    private Double week52High;
    private Double week52Low;
}
