package com.fxrate.platform.rate.controller;

import com.fxrate.platform.common.dto.ErrorResponse;
import com.fxrate.platform.rate.dto.RateResponse;
import com.fxrate.platform.rate.service.RateQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Locale;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
public class RateController {

    private final RateQueryService rateQueryService;

    /**
     * Exposes GET /api/rates
     * Returns all latest cached rates.
     */
    @GetMapping
    public List<RateResponse> getAllRates() {
        return rateQueryService.getAllRates();
    }

    /**
     * Exposes GET /api/rates/{base}/{quote}
     * Returns the latest rate for the resolved pair (normalized to uppercase).
     */
    @GetMapping("/{base}/{quote}")
    public ResponseEntity<?> getRateByPair(
            @PathVariable String base,
            @PathVariable String quote) {
        
        String pair = base.toUpperCase(Locale.ROOT) + "/" + quote.toUpperCase(Locale.ROOT);
        var rateOpt = rateQueryService.getRateByPair(pair);
        if (rateOpt.isPresent()) {
            return ResponseEntity.ok(rateOpt.get());
        } else {
            ErrorResponse error = new ErrorResponse(
                    "RATE_NOT_FOUND",
                    "No rate found for pair " + pair
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}
