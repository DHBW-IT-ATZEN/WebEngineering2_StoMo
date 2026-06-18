package com.dhbw.webeng2.stomo.controller;

import com.dhbw.webeng2.stomo.model.dto.CompanyOverviewDto;
import com.dhbw.webeng2.stomo.model.dto.GlobalQuoteDto;
import com.dhbw.webeng2.stomo.model.dto.PriceSeriesDto;
import com.dhbw.webeng2.stomo.model.dto.SearchTickerDto;
import com.dhbw.webeng2.stomo.model.dto.TickerDto;
import com.dhbw.webeng2.stomo.service.BannerService;
import com.dhbw.webeng2.stomo.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final BannerService bannerService;

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
}
