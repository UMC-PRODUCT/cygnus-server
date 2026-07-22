package com.umc.product.global.websocket.handler;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public final class WebSocketHttpStatusSerializer extends JsonSerializer<Integer> {

    @Override
    public void serialize(Integer value, JsonGenerator generator, SerializerProvider serializers)
        throws IOException {
        generator.writeRawValue(Integer.toString(value));
    }
}
