package com.belnarto.trustpilotscraper.it;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.NumberFormat;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import redis.embedded.RedisServer;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TrustpilotScraperAppItTests {

    private static MockWebServer mockTrustpilotBackEnd;

    private static RedisServer redisServer;

    @Autowired
    WebTestClient webClient;

    @Value("${trustpilot.default-error-message}")
    private String defaultErrorMessage;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("trustpilot.base-url", () -> "http://localhost:" + mockTrustpilotBackEnd.getPort());
    }

    @BeforeAll
    static void setUp() throws IOException {
        mockTrustpilotBackEnd = new MockWebServer();
        mockTrustpilotBackEnd.start();

        redisServer = RedisServer.builder()
            .setting("maxmemory 128M") // https://github.com/kstyrc/embedded-redis/issues/51
            .build();
        redisServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockTrustpilotBackEnd.shutdown();

        redisServer.stop();
    }

    @Test
    void getReviewSuccessful() throws IOException {
        String path = "/mockwebserver/successfulWithValues.html";
        String mockHtml = IOUtils.toString(requireNonNull(this.getClass().getResourceAsStream(path)), UTF_8);

        mockTrustpilotBackEnd.enqueue(new MockResponse()
            .setBody(mockHtml)
            .addHeader("Content-Type", "text/html; charset=utf-8"));

        int reviewsCount = 1274;
        double rating = 4.9;

        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMaximumFractionDigits(1);
        numberFormat.setRoundingMode(RoundingMode.FLOOR);

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
    void getReviewNotFound() throws IOException {
        String path = "/mockwebserver/successfulDomainNotFound.html";
        String mockHtml = IOUtils.toString(requireNonNull(this.getClass().getResourceAsStream(path)), UTF_8);

        mockTrustpilotBackEnd.enqueue(new MockResponse()
            .setStatus("HTTP/1.1 404 NOT_FOUND")
            .setBody(mockHtml)
            .addHeader("Content-Type", "text/html; charset=utf-8"));

        String domain = "anyDomain";
        String errorMessage = "Review for domain: " + domain + " was not found";

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
    void getReviewServerError() throws IOException {
        String path = "/mockwebserver/remoteError.html";
        String mockHtml = IOUtils.toString(requireNonNull(this.getClass().getResourceAsStream(path)), UTF_8);

        mockTrustpilotBackEnd.enqueue(new MockResponse()
            .setStatus("HTTP/1.1 403 FORBIDDEN")
            .setBody(mockHtml)
            .addHeader("Content-Type", "text/html; charset=utf-8"));

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
