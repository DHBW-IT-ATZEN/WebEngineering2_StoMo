package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.model.dto.CompanyOverviewDto;
import com.dhbw.webeng2.stomo.model.dto.GlobalQuoteDto;
import com.dhbw.webeng2.stomo.model.dto.PriceSeriesDto;
import com.dhbw.webeng2.stomo.model.dto.SearchTickerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Aggregates the market-data sources:
 *  - Finnhub (ReportService) for the real-time quote
 *  - Yahoo (PriceHistoryService) for the cached intraday 30-minute series
 *  - Alpha Vantage (StockService) for company overview and ticker search
 */
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final ReportService finnhub;
    private final StockService alphaVantage;
    private final PriceHistoryService priceHistory;

    public GlobalQuoteDto getQuote(String symbol) {
        return finnhub.getQuote(symbol);
    }

    public PriceSeriesDto getSeries(String symbol) {
        return priceHistory.getSeries(symbol);
    }

    public CompanyOverviewDto getOverview(String symbol) {
        return alphaVantage.getOverview(symbol);
    }

    public List<SearchTickerDto> search(String keywords) {
        return alphaVantage.search(keywords);
    }
}
