package com.dhbw.webeng2.stomo.model.enums;

import lombok.Getter;

@Getter
public enum Interval {
    DAILY("TIME_SERIES_DAILY", "Time Series (Daily)"),
    WEEKLY("TIME_SERIES_WEEKLY", "Weekly Time Series"),
    MONTHLY("TIME_SERIES_MONTHLY", "Monthly Time Series");

    private final String apiFunction;
    private final String seriesKey;

    Interval(String apiFunction, String seriesKey) {
        this.apiFunction = apiFunction;
        this.seriesKey = seriesKey;
    }
}
