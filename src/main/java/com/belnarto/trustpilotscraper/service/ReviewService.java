package com.belnarto.trustpilotscraper.service;

import com.belnarto.trustpilotscraper.dto.ReviewDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ReviewService {

    public Mono<ReviewDto> getReviewByDomain(final String domain) {
        try {
            return Mono.just(new ReviewDto(1, 1.1123));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

}
