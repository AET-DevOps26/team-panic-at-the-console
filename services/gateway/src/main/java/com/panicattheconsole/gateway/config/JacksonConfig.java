package com.panicattheconsole.gateway.config;

import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class JacksonConfig {

    @Bean
    JsonNullableModule jsonNullableModule() {
        return new JsonNullableModule();
    }
}
