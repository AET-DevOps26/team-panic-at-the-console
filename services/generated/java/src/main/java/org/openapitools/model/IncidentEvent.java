package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One entry from an incident&#39;s append-only Event Log.
 */

@Schema(name = "IncidentEvent", description = "One entry from an incident's append-only Event Log.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class IncidentEvent {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime timestamp;

  private String type;

  private String description;

  private @Nullable String newValue;

  public IncidentEvent() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IncidentEvent(OffsetDateTime timestamp, String type, String description) {
    this.timestamp = timestamp;
    this.type = type;
    this.description = description;
  }

  public IncidentEvent timestamp(OffsetDateTime timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  /**
   * Get timestamp
   * @return timestamp
   */
  @NotNull @Valid
  @Schema(name = "timestamp", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("timestamp")
  public OffsetDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(OffsetDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public IncidentEvent type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull
  @Schema(name = "type", example = "status_changed", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public IncidentEvent description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  @NotNull
  @Schema(name = "description", example = "status: open -> investigating", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public IncidentEvent newValue(@Nullable String newValue) {
    this.newValue = newValue;
    return this;
  }

  /**
   * New value after the change: the new status for status_changed entries and the new severity for severity_changed entries. Lets clients color-code timeline entries without parsing the description. Absent for other entry types and for events stored before this field existed.
   * @return newValue
   */

  @Schema(name = "newValue", example = "investigating", description = "New value after the change: the new status for status_changed entries and the new severity for severity_changed entries. Lets clients color-code timeline entries without parsing the description. Absent for other entry types and for events stored before this field existed.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("newValue")
  public @Nullable String getNewValue() {
    return newValue;
  }

  public void setNewValue(@Nullable String newValue) {
    this.newValue = newValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IncidentEvent incidentEvent = (IncidentEvent) o;
    return Objects.equals(this.timestamp, incidentEvent.timestamp) &&
        Objects.equals(this.type, incidentEvent.type) &&
        Objects.equals(this.description, incidentEvent.description) &&
        Objects.equals(this.newValue, incidentEvent.newValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, type, description, newValue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IncidentEvent {\n");
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    newValue: ").append(toIndentedString(newValue)).append("\n");
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
