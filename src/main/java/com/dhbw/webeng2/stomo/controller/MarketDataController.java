package com.dhbw.webeng2.stomo.controller;

import com.dhbw.webeng2.stomo.model.dto.CompanyOverviewDto;
import com.dhbw.webeng2.stomo.model.dto.GlobalQuoteDto;
import com.dhbw.webeng2.stomo.model.dto.PriceSeriesDto;
import com.dhbw.webeng2.stomo.model.dto.SearchTickerDto;
import com.dhbw.webeng2.stomo.model.dto.TickerDto;
import com.dhbw.webeng2.stomo.service.BannerService;
import com.dhbw.webeng2.stomo.service.ExchangeRateService;
import com.dhbw.webeng2.stomo.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final BannerService bannerService;
    private final ExchangeRateService exchangeRateService;

    @GetMapping("/quote/{symbol}")
    public ResponseEntity<GlobalQuoteDto> getQuote(@PathVariable String symbol) {
        return ResponseEntity.ok(marketDataService.getQuote(symbol));
    }

    @GetMapping("/history/{symbol}")
    public ResponseEntity<PriceSeriesDto> getHistory(@PathVariable String symbol) {
        return ResponseEntity.ok(marketDataService.getSeries(symbol));
    }

    @GetMapping("/overview/{symbol}")
    public ResponseEntity<CompanyOverviewDto> getOverview(@PathVariable String symbol) {
        return ResponseEntity.ok(marketDataService.getOverview(symbol));
    }

    @GetMapping("/search")
    public ResponseEntity<List<SearchTickerDto>> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(marketDataService.search(query));
    }

    /** Homepage ticker banner: top-watched symbols (or a curated default set), with live quotes. */
    @GetMapping("/movers")
    public ResponseEntity<List<TickerDto>> movers() {
        return ResponseEntity.ok(bannerService.getBanner());
    }

    /** Start-of-day FX rates (USD per unit) plus the currencies offered in the UI picker. */
    @GetMapping("/fx")
    public ResponseEntity<Map<String, Object>> fx() {
        return ResponseEntity.ok(Map.of(
                "base", exchangeRateService.getBase(),
                "asOfDate", LocalDate.now().toString(),
                "rates", exchangeRateService.getRatesToUsd(),
                "displayOptions", exchangeRateService.getDisplayOptions()));
    }
}
