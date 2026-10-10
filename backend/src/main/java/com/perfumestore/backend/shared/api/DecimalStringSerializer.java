package com.perfumestore.backend.shared.api;

import java.math.BigDecimal;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class DecimalStringSerializer extends ValueSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator generator, SerializationContext serializers) {
        generator.writeString(value.toPlainString());
    }
}
