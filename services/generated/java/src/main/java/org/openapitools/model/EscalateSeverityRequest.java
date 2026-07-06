package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.model.Severity;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Request to set incident severity (any level, higher or lower).
 */

@Schema(name = "EscalateSeverityRequest", description = "Request to set incident severity (any level, higher or lower).")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class EscalateSeverityRequest {

  private Severity severity;

  public EscalateSeverityRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EscalateSeverityRequest(Severity severity) {
    this.severity = severity;
  }

  public EscalateSeverityRequest severity(Severity severity) {
    this.severity = severity;
    return this;
  }

  /**
   * Get severity
   * @return severity
   */
  @NotNull @Valid
  @Schema(name = "severity", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("severity")
  public Severity getSeverity() {
    return severity;
  }

  public void setSeverity(Severity severity) {
    this.severity = severity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EscalateSeverityRequest escalateSeverityRequest = (EscalateSeverityRequest) o;
    return Objects.equals(this.severity, escalateSeverityRequest.severity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(severity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EscalateSeverityRequest {\n");
    sb.append("    severity: ").append(toIndentedString(severity)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
