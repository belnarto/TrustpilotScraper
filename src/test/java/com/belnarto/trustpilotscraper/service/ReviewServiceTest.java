package com.belnarto.trustpilotscraper.service;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.belnarto.trustpilotscraper.config.TestRedisConfiguration;
import com.belnarto.trustpilotscraper.dto.ReviewDto;
import com.belnarto.trustpilotscraper.scraper.ReviewScraper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestRedisConfiguration.class)
class ReviewServiceTest {

    @Autowired
    ReviewService reviewService;

    @MockBean
    ReviewScraper reviewScraper;

    @Test
    void getReviewByDomainNoCache() {
        int reviewsCount = 100;
        double rating = 4.3;

        when(reviewScraper.scrapForReviewByDomain(any()))
            .thenReturn(Mono.just(new ReviewDto(reviewsCount, rating)));

        Mono<ReviewDto> review = reviewService.getReviewByDomain("anyDomain1");
        StepVerifier.create(review)
            .expectNextMatches(r -> r.getReviewsCount() == reviewsCount && r.getRating() == rating)
            .verifyComplete();

        review = reviewService.getReviewByDomain("anyDomain2");
        StepVerifier.create(review)
            .expectNextMatches(r -> r.getReviewsCount() == reviewsCount && r.getRating() == rating)
            .verifyComplete();

        verify(reviewScraper, times(2)).scrapForReviewByDomain(any());
    }

    @Test
    void getReviewByDomainFromCache() {
        int reviewsCount = 100;
        double rating = 4.3;
        String domain = "anyDomain";

        when(reviewScraper.scrapForReviewByDomain(any()))
            .thenReturn(Mono.just(new ReviewDto(reviewsCount, rating)));

        Mono<ReviewDto> review = reviewService.getReviewByDomain(domain);
        StepVerifier.create(review)
            .expectNextMatches(r -> r.getReviewsCount() == reviewsCount && r.getRating() == rating)
            .verifyComplete();

        review = reviewService.getReviewByDomain(domain);
        StepVerifier.create(review)
            .expectNextMatches(r -> r.getReviewsCount() == reviewsCount && r.getRating() == rating)
            .verifyComplete();

        verify(reviewScraper, times(1)).scrapForReviewByDomain(any());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void getReviewByDomainNoCacheBecauseShortTtl() {
        int reviewsCount = 100;
        double rating = 4.3;
        String domain = "anyDomainShortTtl";

        long cacheTtlSecInitial = (long) ReflectionTestUtils.getField(reviewService, "cacheTtlSec");
        ReflectionTestUtils.setField(reviewService, "cacheTtlSec", 1);

        when(reviewScraper.scrapForReviewByDomain(any()))
            .thenReturn(Mono.just(new ReviewDto(reviewsCount, rating)));

        Mono<ReviewDto> review = reviewService.getReviewByDomain(domain);
        StepVerifier.create(review)
            .expectNextMatches(r -> r.getReviewsCount() == reviewsCount && r.getRating() == rating)
            .verifyComplete();

        await()
            .pollDelay(Duration.ofMillis(1001))
            .until(() -> {
                Mono<ReviewDto> reviewForSameDomain = reviewService.getReviewByDomain(domain);
                StepVerifier.create(reviewForSameDomain)
                    .expectNextMatches(r -> r.getReviewsCount() == reviewsCount && r.getRating() == rating)
                    .verifyComplete();
                return true;
            });

        verify(reviewScraper, times(2)).scrapForReviewByDomain(any());

        ReflectionTestUtils.setField(reviewService, "cacheTtlSec", cacheTtlSecInitial);
    }

}