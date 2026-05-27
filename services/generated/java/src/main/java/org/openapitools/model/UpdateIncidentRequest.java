package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.model.IncidentStatus;
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
 * Partial update for an incident&#39;s mutable fields.
 */

@Schema(name = "UpdateIncidentRequest", description = "Partial update for an incident's mutable fields.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class UpdateIncidentRequest {

  private @Nullable IncidentStatus status;

  private @Nullable Severity severity;

  public UpdateIncidentRequest status(@Nullable IncidentStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @Valid
  @Schema(name = "status", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public @Nullable IncidentStatus getStatus() {
    return status;
  }

  public void setStatus(@Nullable IncidentStatus status) {
    this.status = status;
  }

  public UpdateIncidentRequest severity(@Nullable Severity severity) {
    this.severity = severity;
    return this;
  }

  /**
   * Get severity
   * @return severity
   */
  @Valid
  @Schema(name = "severity", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("severity")
  public @Nullable Severity getSeverity() {
    return severity;
  }

  public void setSeverity(@Nullable Severity severity) {
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
    UpdateIncidentRequest updateIncidentRequest = (UpdateIncidentRequest) o;
    return Objects.equals(this.status, updateIncidentRequest.status) &&
        Objects.equals(this.severity, updateIncidentRequest.severity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, severity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateIncidentRequest {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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
