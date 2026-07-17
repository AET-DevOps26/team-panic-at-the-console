package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.model.RuleOperator;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A single match condition. The field is a dotted path into the event, rooted at an object exposing &#x60;source&#x60;, &#x60;eventType&#x60; and &#x60;payload&#x60; (the raw webhook body), e.g. &#x60;payload.workflow_run.conclusion&#x60;.
 */

@Schema(name = "RuleCondition", description = "A single match condition. The field is a dotted path into the event, rooted at an object exposing `source`, `eventType` and `payload` (the raw webhook body), e.g. `payload.workflow_run.conclusion`.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class RuleCondition {

  private String field;

  private RuleOperator operator;

  private @Nullable String value;

  public RuleCondition() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RuleCondition(String field, RuleOperator operator) {
    this.field = field;
    this.operator = operator;
  }

  public RuleCondition field(String field) {
    this.field = field;
    return this;
  }

  /**
   * Dotted path into the event, e.g. `payload.workflow_run.conclusion`.
   * @return field
   */
  @NotNull
  @Schema(name = "field", example = "payload.workflow_run.conclusion", description = "Dotted path into the event, e.g. `payload.workflow_run.conclusion`.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("field")
  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  public RuleCondition operator(RuleOperator operator) {
    this.operator = operator;
    return this;
  }

  /**
   * Get operator
   * @return operator
   */
  @NotNull @Valid
  @Schema(name = "operator", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("operator")
  public RuleOperator getOperator() {
    return operator;
  }

  public void setOperator(RuleOperator operator) {
    this.operator = operator;
  }

  public RuleCondition value(@Nullable String value) {
    this.value = value;
    return this;
  }

  /**
   * Comparison value. For `in` this is a comma-separated list. Ignored by `exists`/`not_exists`.
   * @return value
   */

  @Schema(name = "value", example = "failure", description = "Comparison value. For `in` this is a comma-separated list. Ignored by `exists`/`not_exists`.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("value")
  public @Nullable String getValue() {
    return value;
  }

  public void setValue(@Nullable String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RuleCondition ruleCondition = (RuleCondition) o;
    return Objects.equals(this.field, ruleCondition.field) &&
        Objects.equals(this.operator, ruleCondition.operator) &&
        Objects.equals(this.value, ruleCondition.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(field, operator, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RuleCondition {\n");
    sb.append("    field: ").append(toIndentedString(field)).append("\n");
    sb.append("    operator: ").append(toIndentedString(operator)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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
