package com.belnarto.trustpilotscraper.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.NumberFormat;

public class RatingSerializer extends StdSerializer<Double> {

    private final NumberFormat numberFormat = NumberFormat.getNumberInstance();

    public RatingSerializer() {
        this(null);
    }

    public RatingSerializer(Class<Double> t) {
        super(t);
        numberFormat.setMaximumFractionDigits(1);
        numberFormat.setRoundingMode(RoundingMode.FLOOR);
    }

    @Override
    public void serialize(Double value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeNumber(numberFormat.format(value));
    }

}
