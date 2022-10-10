package com.belnarto.trustpilotscraper.scraper;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.springframework.util.StringUtils.hasText;

import com.belnarto.trustpilotscraper.dto.ReviewDto;
import com.belnarto.trustpilotscraper.exception.ReviewNotFoundException;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class ReviewScraperTest {

    static MockWebServer mockBackEnd;

    @Autowired
    ReviewScraper reviewScraper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("trustpilot.base-url", () -> "http://localhost:" + mockBackEnd.getPort());
    }

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void getReviewSuccessfulWithValues() throws IOException {
        String path = "/mockwebserver/successfulWithValues.html";
        String mockHtml = IOUtils.toString(requireNonNull(this.getClass().getResourceAsStream(path)), UTF_8);

        mockBackEnd.enqueue(new MockResponse()
            .setBody(mockHtml)
            .addHeader("Content-Type", "text/html; charset=utf-8"));

        Mono<ReviewDto> reviewDtoMono = reviewScraper.scrapForReviewByDomain("successfulWithValues");

        StepVerifier.create(reviewDtoMono)
            .expectNextMatches(r -> r.getReviewsCount() == 1274 && r.getRating() == 4.9)
            .verifyComplete();
    }

    @Test
    void getReviewSuccessfulWithoutValues() throws IOException {
        String path = "/mockwebserver/successfulWithoutValues.html";
        String mockHtml = IOUtils.toString(requireNonNull(this.getClass().getResourceAsStream(path)), UTF_8);

        mockBackEnd.enqueue(new MockResponse()
            .setBody(mockHtml)
            .addHeader("Content-Type", "text/html; charset=utf-8"));

        Mono<ReviewDto> reviewDtoMono = reviewScraper.scrapForReviewByDomain("successfulWithoutValues");

        StepVerifier.create(reviewDtoMono)
            .expectNextMatches(r -> r.getReviewsCount() == 0 && r.getRating() == 0.0)
            .verifyComplete();
    }

    @Test
    void getReviewSuccessfulDomainNotFound() throws IOException {
        String path = "/mockwebserver/successfulDomainNotFound.html";
        String mockHtml = IOUtils.toString(requireNonNull(this.getClass().getResourceAsStream(path)), UTF_8);

        mockBackEnd.enqueue(new MockResponse()
            .setStatus("HTTP/1.1 404 NOT_FOUND")
            .setBody(mockHtml)
            .addHeader("Content-Type", "text/html; charset=utf-8"));

        Mono<ReviewDto> reviewDtoMono = reviewScraper.scrapForReviewByDomain("successfulDomainNotFound");

        StepVerifier.create(reviewDtoMono)
            .expectErrorMatches(e -> e instanceof ReviewNotFoundException && hasText(e.getMessage()))
            .verify();
    }

    @Test
    void getReviewWithRemoteError() throws IOException {
        String path = "/mockwebserver/remoteError.html";
        String mockHtml = IOUtils.toString(requireNonNull(this.getClass().getResourceAsStream(path)), UTF_8);

        mockBackEnd.enqueue(new MockResponse()
            .setStatus("HTTP/1.1 403 FORBIDDEN")
            .setBody(mockHtml)
            .addHeader("Content-Type", "text/html; charset=utf-8"));

        Mono<ReviewDto> reviewDtoMono = reviewScraper.scrapForReviewByDomain("remoteError");

        StepVerifier.create(reviewDtoMono)
            .expectErrorMatches(e -> e instanceof WebClientResponseException)
            .verify();
    }

}