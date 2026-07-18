package io.virtualsms.sdk.internal;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/**
 * Shared Jackson {@link ObjectMapper}, configured to read/write plain-field
 * model classes (see {@code io.virtualsms.sdk.model}) and to translate
 * between the SDK's idiomatic camelCase fields and the API's snake_case JSON
 * automatically.
 */
public final class Json {

    public static final ObjectMapper MAPPER = build();

    private Json() {
    }

    private static ObjectMapper build() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        // Mirrors axios/JSON.stringify dropping `undefined` optional fields:
        // a request body Map with a null value for an unset optional param
        // (e.g. no `service` on a full_access rental) omits the key entirely
        // rather than sending "field": null.
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
