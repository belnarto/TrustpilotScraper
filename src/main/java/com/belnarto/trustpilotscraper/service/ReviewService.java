package com.belnarto.trustpilotscraper.service;

import com.belnarto.trustpilotscraper.dto.ReviewDto;
import com.belnarto.trustpilotscraper.scraper.ReviewScraper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewScraper reviewScraper;

    public Mono<ReviewDto> getReviewByDomain(final String domain) {
        return reviewScraper.scrapForReviewByDomain(domain);
    }

}
