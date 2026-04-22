package com.dhbw.webeng2.stomo.model.enums;

import lombok.Getter;

@Getter
public enum Interval {
    DAILY("TIME_SERIES_DAILY"),
    WEEKLY("TIME_SERIES_WEEKLY"),
    MONTHLY("TIME_SERIES_MONTHLY");

    private final String apiFunction;

    Interval(String apiFunction) {
        this.apiFunction = apiFunction;
    }
}
