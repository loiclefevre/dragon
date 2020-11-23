package com.oracle.dragon.stacks.requirements;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.oracle.dragon.stacks.EnvironmentRequirement;

import java.io.IOException;

public class EnvironmentRequirementDeserializer extends StdDeserializer<EnvironmentRequirement> {
    public EnvironmentRequirementDeserializer() {
        this(null);
    }

    public EnvironmentRequirementDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public EnvironmentRequirement deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        final JsonNode keyNode = jp.getCodec().readTree(jp);

        return Enum.valueOf(EnvironmentRequirement.class, keyNode.asText().toLowerCase());
    }
}
