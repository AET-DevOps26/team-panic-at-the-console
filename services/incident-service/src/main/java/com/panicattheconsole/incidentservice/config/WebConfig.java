package com.panicattheconsole.incidentservice.config;

import org.openapitools.model.IncidentStatus;
import org.openapitools.model.Severity;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new IncidentStatusConverter());
        registry.addConverter(new SeverityConverter());
    }

    private static final class IncidentStatusConverter implements Converter<String, IncidentStatus> {
        @Override
        public IncidentStatus convert(String source) {
            if (source == null) {
                return null;
            }
            return IncidentStatus.fromValue(source);
        }
    }

    private static final class SeverityConverter implements Converter<String, Severity> {
        @Override
        public Severity convert(String source) {
            if (source == null) {
                return null;
            }
            return Severity.fromValue(source);
        }
    }
}
