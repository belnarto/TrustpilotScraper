package com.belnarto.trustpilotscraper.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.belnarto.trustpilotscraper.dto.ReviewDto;
import com.belnarto.trustpilotscraper.exception.ReviewNotFoundException;
import com.belnarto.trustpilotscraper.service.ReviewService;
import java.math.RoundingMode;
import java.text.NumberFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReviewControllerTest {

    @Autowired
    WebTestClient webClient;

    @Value("${trustpilot.default-error-message}")
    private String defaultErrorMessage;

    @MockBean
    ReviewService reviewService;

    @Test
    void getReviewSuccessful() {
        int reviewsCount = 15;
        double rating = 4.512312;

        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMaximumFractionDigits(1);
        numberFormat.setRoundingMode(RoundingMode.FLOOR);

        when(reviewService.getReviewByDomain(anyString()))
            .thenReturn(Mono.just(new ReviewDto(reviewsCount, rating)));

        webClient.get().uri("/reviews/anyDomain")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.reviewsCount").isEqualTo(reviewsCount)
            .jsonPath("$.rating").isEqualTo(numberFormat.format(rating));
    }

    @Test
    void getReviewNotFound() {
        String domain = "anyDomain";
        String errorMessage = "Review for domain: " + domain + " was not found";

        when(reviewService.getReviewByDomain(anyString()))
            .thenReturn(Mono.error(new ReviewNotFoundException(errorMessage)));

        webClient.get().uri("/reviews/" + domain)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.message").isEqualTo(errorMessage);
    }

    @Test
    void getReviewServerError() {
        when(reviewService.getReviewByDomain(anyString()))
            .thenReturn(Mono.error(new Exception(defaultErrorMessage)));

        webClient.get().uri("/reviews/remoteError")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().is5xxServerError()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(500)
            .jsonPath("$.message").isEqualTo(defaultErrorMessage);
    }

}