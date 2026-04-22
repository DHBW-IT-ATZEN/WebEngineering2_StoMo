package com.dhbw.webeng2.stomo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StockService {
    @Value("${ALPHAVANTAGE_API_KEY}")
    private String apiKey;

    public void printKey() {
        System.out.println("Mein Key ist: " + apiKey);
    }
}
