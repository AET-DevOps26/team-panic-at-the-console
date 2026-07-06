package com.panicattheconsole.notificationservice.config;

import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the {@link JsonNullableModule} so Jackson unwraps the {@code JsonNullable<T>}
 * fields the OpenAPI generator emits for nullable properties (for example a notification's
 * {@code recipientId}). Without it those fields serialize as the wrapper object
 * ({@code {"present":...,"undefined":...}}) rather than the value or null.
 */
@Configuration
class JacksonConfig {

    @Bean
    JsonNullableModule jsonNullableModule() {
        return new JsonNullableModule();
    }
}
