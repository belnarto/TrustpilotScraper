package com.belnarto.trustpilotscraper.service;

import com.belnarto.trustpilotscraper.dto.ReviewDto;
import com.belnarto.trustpilotscraper.scraper.ReviewScraper;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewScraper reviewScraper;
    private final ReactiveRedisTemplate<String, ReviewDto> reactiveRedisTemplate;

    @Value("${trustpilot.cache-ttl-sec}")
    private long cacheTtlSec;

    public Mono<ReviewDto> getReviewByDomain(final String domain) {
        return reactiveRedisTemplate.opsForValue().get(domain)
            .switchIfEmpty(Mono.defer(() -> getReviewAndAddToCache(domain)));
    }

    private Mono<ReviewDto> getReviewAndAddToCache(String domain) {
        try {
            return reviewScraper.scrapForReviewByDomain(domain)
                .map(response -> {
                    reactiveRedisTemplate.opsForValue()
                        .set(domain, response, Duration.ofSeconds(cacheTtlSec)).subscribe();
                    return response;
                });
        } catch (Throwable e) {
            return Mono.error(e);
        }
    }

}
