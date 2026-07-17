package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.model.RuleCondition;
import org.openapitools.model.RuleMetadataField;
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
 * Definition used to create or replace a rule.
 */

@Schema(name = "RuleInput", description = "Definition used to create or replace a rule.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class RuleInput {

  private String name;

  private Boolean enabled = true;

  private Integer priority = 100;

  private @Nullable String source;

  @Valid
  private List<@Valid RuleCondition> conditions = new ArrayList<>();

  private Severity severity;

  private String titleTemplate;

  private @Nullable String descriptionTemplate;

  @Valid
  private List<@Valid RuleMetadataField> metadataFields = new ArrayList<>();

  private @Nullable String dedupKeyTemplate;

  public RuleInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RuleInput(String name, Severity severity, String titleTemplate) {
    this.name = name;
    this.severity = severity;
    this.titleTemplate = titleTemplate;
  }

  public RuleInput name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Human-readable rule name.
   * @return name
   */
  @NotNull
  @Schema(name = "name", example = "GitHub CI failures", description = "Human-readable rule name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public RuleInput enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Get enabled
   * @return enabled
   */

  @Schema(name = "enabled", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public RuleInput priority(Integer priority) {
    this.priority = priority;
    return this;
  }

  /**
   * Lower runs first; the first matching enabled rule wins.
   * @return priority
   */

  @Schema(name = "priority", example = "100", description = "Lower runs first; the first matching enabled rule wins.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("priority")
  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public RuleInput source(@Nullable String source) {
    this.source = source;
    return this;
  }

  /**
   * Only evaluate events from this source slug; omit to match any source.
   * @return source
   */

  @Schema(name = "source", example = "github", description = "Only evaluate events from this source slug; omit to match any source.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("source")
  public @Nullable String getSource() {
    return source;
  }

  public void setSource(@Nullable String source) {
    this.source = source;
  }

  public RuleInput conditions(List<@Valid RuleCondition> conditions) {
    this.conditions = conditions;
    return this;
  }

  public RuleInput addConditionsItem(RuleCondition conditionsItem) {
    if (this.conditions == null) {
      this.conditions = new ArrayList<>();
    }
    this.conditions.add(conditionsItem);
    return this;
  }

  /**
   * All conditions must match (logical AND). An empty list matches every event.
   * @return conditions
   */
  @Valid
  @Schema(name = "conditions", description = "All conditions must match (logical AND). An empty list matches every event.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("conditions")
  public List<@Valid RuleCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<@Valid RuleCondition> conditions) {
    this.conditions = conditions;
  }

  public RuleInput severity(Severity severity) {
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

  public RuleInput titleTemplate(String titleTemplate) {
    this.titleTemplate = titleTemplate;
    return this;
  }

  /**
   * Incident title; supports `{{dotted.path}}` placeholders.
   * @return titleTemplate
   */
  @NotNull
  @Schema(name = "titleTemplate", example = "CI failure: {{payload.workflow_run.name}}", description = "Incident title; supports `{{dotted.path}}` placeholders.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("titleTemplate")
  public String getTitleTemplate() {
    return titleTemplate;
  }

  public void setTitleTemplate(String titleTemplate) {
    this.titleTemplate = titleTemplate;
  }

  public RuleInput descriptionTemplate(@Nullable String descriptionTemplate) {
    this.descriptionTemplate = descriptionTemplate;
    return this;
  }

  /**
   * Optional leading description text; supports `{{dotted.path}}` placeholders (Markdown).
   * @return descriptionTemplate
   */

  @Schema(name = "descriptionTemplate", description = "Optional leading description text; supports `{{dotted.path}}` placeholders (Markdown).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("descriptionTemplate")
  public @Nullable String getDescriptionTemplate() {
    return descriptionTemplate;
  }

  public void setDescriptionTemplate(@Nullable String descriptionTemplate) {
    this.descriptionTemplate = descriptionTemplate;
  }

  public RuleInput metadataFields(List<@Valid RuleMetadataField> metadataFields) {
    this.metadataFields = metadataFields;
    return this;
  }

  public RuleInput addMetadataFieldsItem(RuleMetadataField metadataFieldsItem) {
    if (this.metadataFields == null) {
      this.metadataFields = new ArrayList<>();
    }
    this.metadataFields.add(metadataFieldsItem);
    return this;
  }

  /**
   * Fields rendered as a labelled Markdown list appended to the description.
   * @return metadataFields
   */
  @Valid
  @Schema(name = "metadataFields", description = "Fields rendered as a labelled Markdown list appended to the description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("metadataFields")
  public List<@Valid RuleMetadataField> getMetadataFields() {
    return metadataFields;
  }

  public void setMetadataFields(List<@Valid RuleMetadataField> metadataFields) {
    this.metadataFields = metadataFields;
  }

  public RuleInput dedupKeyTemplate(@Nullable String dedupKeyTemplate) {
    this.dedupKeyTemplate = dedupKeyTemplate;
    return this;
  }

  /**
   * Placeholder template computing a dedup key; at most one incident is created per (rule, key). Empty falls back to the event id, e.g. `{{payload.workflow_run.id}}`.
   * @return dedupKeyTemplate
   */

  @Schema(name = "dedupKeyTemplate", example = "{{payload.workflow_run.id}}", description = "Placeholder template computing a dedup key; at most one incident is created per (rule, key). Empty falls back to the event id, e.g. `{{payload.workflow_run.id}}`.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("dedupKeyTemplate")
  public @Nullable String getDedupKeyTemplate() {
    return dedupKeyTemplate;
  }

  public void setDedupKeyTemplate(@Nullable String dedupKeyTemplate) {
    this.dedupKeyTemplate = dedupKeyTemplate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RuleInput ruleInput = (RuleInput) o;
    return Objects.equals(this.name, ruleInput.name) &&
        Objects.equals(this.enabled, ruleInput.enabled) &&
        Objects.equals(this.priority, ruleInput.priority) &&
        Objects.equals(this.source, ruleInput.source) &&
        Objects.equals(this.conditions, ruleInput.conditions) &&
        Objects.equals(this.severity, ruleInput.severity) &&
        Objects.equals(this.titleTemplate, ruleInput.titleTemplate) &&
        Objects.equals(this.descriptionTemplate, ruleInput.descriptionTemplate) &&
        Objects.equals(this.metadataFields, ruleInput.metadataFields) &&
        Objects.equals(this.dedupKeyTemplate, ruleInput.dedupKeyTemplate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, enabled, priority, source, conditions, severity, titleTemplate, descriptionTemplate, metadataFields, dedupKeyTemplate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RuleInput {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
    sb.append("    conditions: ").append(toIndentedString(conditions)).append("\n");
    sb.append("    severity: ").append(toIndentedString(severity)).append("\n");
    sb.append("    titleTemplate: ").append(toIndentedString(titleTemplate)).append("\n");
    sb.append("    descriptionTemplate: ").append(toIndentedString(descriptionTemplate)).append("\n");
    sb.append("    metadataFields: ").append(toIndentedString(metadataFields)).append("\n");
    sb.append("    dedupKeyTemplate: ").append(toIndentedString(dedupKeyTemplate)).append("\n");
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
