package com.belnarto.trustpilotscraper.dto;

import com.belnarto.trustpilotscraper.serializer.RatingSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Value;

@Value
public class ReviewDto {

    int reviewsCount;

    @JsonSerialize(using = RatingSerializer.class)
    Double rating;

}
