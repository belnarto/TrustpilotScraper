package com.belnarto.trustpilotscraper.controller;

import com.belnarto.trustpilotscraper.dto.ReviewDto;
import com.belnarto.trustpilotscraper.exception.ReviewNotFoundException;
import com.belnarto.trustpilotscraper.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @Value("${trustpilot.default-error-message}")
    private String defaultErrorMessage;

    @GetMapping(path = "/{domain}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ReviewDto>> getReview(@PathVariable String domain) {
        return reviewService.getReviewByDomain(domain)
            .map(ResponseEntity::ok)
            .onErrorMap(Throwable.class,
                e -> {
                    if (e instanceof ReviewNotFoundException) {
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
                    }
                    log.debug("Unexpected error occurred for domain: " + domain, e);
                    return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, defaultErrorMessage);
                })
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

}
