package com.dhbw.webeng2.stomo.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A cached start-of-day FX rate: how many USD one unit of {@code currency} is worth.
 * Rates are captured from Yahoo once per trading day and held for the rest of the day, so a
 * symbol's price can be normalised to USD (and from there to any display currency).
 */
@Getter
@Setter
@Entity
@Table(name = "exchange_rate")
public class ExchangeRate {

    /** ISO currency code, e.g. EUR, JPY. USD is implicitly 1.0 and not stored. */
    @Id
    @Column(length = 16)
    private String currency;

    /** USD per 1 unit of {@code currency}. */
    @Column(nullable = false)
    private double rateToUsd;

    /** The trading day this rate represents (its start-of-day snapshot). */
    @Column(nullable = false)
    private LocalDate asOfDate;

    @Column(nullable = false)
    private Instant fetchedAt;
}
