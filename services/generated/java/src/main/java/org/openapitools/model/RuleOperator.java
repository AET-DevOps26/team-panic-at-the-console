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
 * How a condition compares the value at its field path against the condition value. `exists`/`not_exists` ignore the value; every other operator compares the field's scalar value as a string.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public enum RuleOperator {

  EQUALS("equals"),

  NOT_EQUALS("not_equals"),

  CONTAINS("contains"),

  NOT_CONTAINS("not_contains"),

  MATCHES("matches"),

  IN("in"),

  EXISTS("exists"),

  NOT_EXISTS("not_exists");

  private final String value;

  RuleOperator(String value) {
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
  public static RuleOperator fromValue(String value) {
    for (RuleOperator b : RuleOperator.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
