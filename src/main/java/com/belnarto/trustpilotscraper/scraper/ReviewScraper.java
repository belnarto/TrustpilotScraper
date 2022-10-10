package com.belnarto.trustpilotscraper.scraper;

import static org.springframework.util.StringUtils.hasText;

import com.belnarto.trustpilotscraper.dto.ReviewDto;
import com.belnarto.trustpilotscraper.exception.ReviewNotFoundException;
import com.belnarto.trustpilotscraper.exception.ReviewParsingException;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ReviewScraper {

    private static final String BUSINESS_UNIT_ID = "business-unit-title";

    private final WebClient webClient;

    @Value("${trustpilot.review-url}")
    private String reviewUrl;

    @Value("${trustpilot.timeout-sec}")
    private int timeoutSec;

    public ReviewScraper(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<ReviewDto> scrapForReviewByDomain(final String domain) {
        if (!hasText(domain)) {
            return Mono.empty();
        }

        log.debug("Start scrapping review from domain: " + domain);

        return webClient
            .get()
            .uri(reviewUrl + domain)
            .retrieve()
            .onStatus(HttpStatus.NOT_FOUND::equals, clientResponse ->
                clientResponse.bodyToMono(String.class)
                    .flatMap(error -> Mono.error(
                        new ReviewNotFoundException("Review for domain: " + domain + " was not found")))
            )
            .bodyToMono(String.class)
            .map(this::parseReviewDocument)
            .timeout(Duration.ofSeconds(timeoutSec));
    }

    private ReviewDto parseReviewDocument(String reviewHtml) {
        Document doc;
        try {
            doc = Jsoup.parse(reviewHtml);
        } catch (Exception e) {
            log.error("Error occurred during parsing review html", e);
            throw new ReviewParsingException(e);
        }

        Element targetElement = doc.getElementById(BUSINESS_UNIT_ID);
        if (targetElement == null) {
            log.debug("Business unit element was not found");
            throw new ReviewParsingException();
        }

        if (targetElement.children().isEmpty() || targetElement.children().size() < 3) {
            log.debug("Business unit has not enough child elements");
            throw new ReviewParsingException();
        }

        int reviewsCount = getReviewsCount(targetElement.child(1))
            .orElseThrow(ReviewParsingException::new);
        log.debug("Reviews count is " + reviewsCount);

        double rating = getRating(targetElement.child(2))
            .orElseThrow(ReviewParsingException::new);
        log.debug("Rating is " + rating);

        return new ReviewDto(reviewsCount, rating);
    }

    private Optional<Integer> getReviewsCount(Element reviewsElement) {
        if (reviewsElement == null) {
            log.debug("Reviews element is not present");
            return Optional.empty();
        }

        String reviewsElementText = reviewsElement.text().trim();
        if (!hasText(reviewsElementText)) {
            log.debug("Reviews element has no text representation");
            return Optional.empty();
        }

        String reviewsCount = reviewsElementText.split(" ")[0].replace(",", "");
        if (!hasText(reviewsCount) || reviewsCount.matches(".*\\D.*")) {
            log.debug("Reviews element has not suitable text representation: \"" + reviewsCount + "\"");
            return Optional.empty();
        }

        return Optional.of(Integer.parseInt(reviewsCount));
    }

    private Optional<Double> getRating(Element ratingElement) {
        if (ratingElement == null) {
            log.debug("Rating element is not present");
            return Optional.empty();
        }

        String rating = ratingElement.text().trim();
        if (!hasText(rating) || !rating.matches("\\d\\.?\\d*")) {
            log.debug("Rating element has not suitable text representation: \"" + rating + "\"");
            return Optional.empty();
        }

        return Optional.of(Double.parseDouble(rating));
    }

}
