package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Category of a notification, derived from the incident event that produced it.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public enum NotificationType {

  INCIDENT_CREATED("INCIDENT_CREATED"),

  SEVERITY_ESCALATED("SEVERITY_ESCALATED"),

  INCIDENT_RESOLVED("INCIDENT_RESOLVED"),

  COMMENT_ADDED("COMMENT_ADDED"),

  INCIDENT_ASSIGNED("INCIDENT_ASSIGNED");

  private final String value;

  NotificationType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static NotificationType fromValue(String value) {
    for (NotificationType b : NotificationType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
