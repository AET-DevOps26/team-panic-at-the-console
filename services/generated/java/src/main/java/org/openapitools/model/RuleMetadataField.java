package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A labelled value pulled from the event into the incident description.
 */

@Schema(name = "RuleMetadataField", description = "A labelled value pulled from the event into the incident description.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class RuleMetadataField {

  private String label;

  private String field;

  public RuleMetadataField() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RuleMetadataField(String label, String field) {
    this.label = label;
    this.field = field;
  }

  public RuleMetadataField label(String label) {
    this.label = label;
    return this;
  }

  /**
   * Get label
   * @return label
   */
  @NotNull
  @Schema(name = "label", example = "Repository", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public RuleMetadataField field(String field) {
    this.field = field;
    return this;
  }

  /**
   * Dotted path into the event.
   * @return field
   */
  @NotNull
  @Schema(name = "field", example = "payload.repository.full_name", description = "Dotted path into the event.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("field")
  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RuleMetadataField ruleMetadataField = (RuleMetadataField) o;
    return Objects.equals(this.label, ruleMetadataField.label) &&
        Objects.equals(this.field, ruleMetadataField.field);
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, field);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RuleMetadataField {\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    field: ").append(toIndentedString(field)).append("\n");
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
