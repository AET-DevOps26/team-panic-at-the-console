package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.openapitools.model.RuleCondition;
import org.openapitools.model.RuleMetadataField;
import org.openapitools.model.Severity;
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
 * A configured incident rule.
 */

@Schema(name = "Rule", description = "A configured incident rule.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class Rule {

  private UUID id;

  private String name;

  private Boolean enabled;

  private Integer priority;

  private @Nullable String source;

  @Valid
  private List<@Valid RuleCondition> conditions = new ArrayList<>();

  private Severity severity;

  private String titleTemplate;

  private @Nullable String descriptionTemplate;

  @Valid
  private List<@Valid RuleMetadataField> metadataFields = new ArrayList<>();

  private @Nullable String dedupKeyTemplate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public Rule() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Rule(UUID id, String name, Boolean enabled, Integer priority, List<@Valid RuleCondition> conditions, Severity severity, String titleTemplate, List<@Valid RuleMetadataField> metadataFields, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    this.id = id;
    this.name = name;
    this.enabled = enabled;
    this.priority = priority;
    this.conditions = conditions;
    this.severity = severity;
    this.titleTemplate = titleTemplate;
    this.metadataFields = metadataFields;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Rule id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull @Valid
  @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Rule name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Rule enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Get enabled
   * @return enabled
   */
  @NotNull
  @Schema(name = "enabled", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Rule priority(Integer priority) {
    this.priority = priority;
    return this;
  }

  /**
   * Get priority
   * @return priority
   */
  @NotNull
  @Schema(name = "priority", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("priority")
  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public Rule source(@Nullable String source) {
    this.source = source;
    return this;
  }

  /**
   * Get source
   * @return source
   */

  @Schema(name = "source", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("source")
  public @Nullable String getSource() {
    return source;
  }

  public void setSource(@Nullable String source) {
    this.source = source;
  }

  public Rule conditions(List<@Valid RuleCondition> conditions) {
    this.conditions = conditions;
    return this;
  }

  public Rule addConditionsItem(RuleCondition conditionsItem) {
    if (this.conditions == null) {
      this.conditions = new ArrayList<>();
    }
    this.conditions.add(conditionsItem);
    return this;
  }

  /**
   * Get conditions
   * @return conditions
   */
  @NotNull @Valid
  @Schema(name = "conditions", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("conditions")
  public List<@Valid RuleCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<@Valid RuleCondition> conditions) {
    this.conditions = conditions;
  }

  public Rule severity(Severity severity) {
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

  public Rule titleTemplate(String titleTemplate) {
    this.titleTemplate = titleTemplate;
    return this;
  }

  /**
   * Get titleTemplate
   * @return titleTemplate
   */
  @NotNull
  @Schema(name = "titleTemplate", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("titleTemplate")
  public String getTitleTemplate() {
    return titleTemplate;
  }

  public void setTitleTemplate(String titleTemplate) {
    this.titleTemplate = titleTemplate;
  }

  public Rule descriptionTemplate(@Nullable String descriptionTemplate) {
    this.descriptionTemplate = descriptionTemplate;
    return this;
  }

  /**
   * Get descriptionTemplate
   * @return descriptionTemplate
   */

  @Schema(name = "descriptionTemplate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("descriptionTemplate")
  public @Nullable String getDescriptionTemplate() {
    return descriptionTemplate;
  }

  public void setDescriptionTemplate(@Nullable String descriptionTemplate) {
    this.descriptionTemplate = descriptionTemplate;
  }

  public Rule metadataFields(List<@Valid RuleMetadataField> metadataFields) {
    this.metadataFields = metadataFields;
    return this;
  }

  public Rule addMetadataFieldsItem(RuleMetadataField metadataFieldsItem) {
    if (this.metadataFields == null) {
      this.metadataFields = new ArrayList<>();
    }
    this.metadataFields.add(metadataFieldsItem);
    return this;
  }

  /**
   * Get metadataFields
   * @return metadataFields
   */
  @NotNull @Valid
  @Schema(name = "metadataFields", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("metadataFields")
  public List<@Valid RuleMetadataField> getMetadataFields() {
    return metadataFields;
  }

  public void setMetadataFields(List<@Valid RuleMetadataField> metadataFields) {
    this.metadataFields = metadataFields;
  }

  public Rule dedupKeyTemplate(@Nullable String dedupKeyTemplate) {
    this.dedupKeyTemplate = dedupKeyTemplate;
    return this;
  }

  /**
   * Get dedupKeyTemplate
   * @return dedupKeyTemplate
   */

  @Schema(name = "dedupKeyTemplate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("dedupKeyTemplate")
  public @Nullable String getDedupKeyTemplate() {
    return dedupKeyTemplate;
  }

  public void setDedupKeyTemplate(@Nullable String dedupKeyTemplate) {
    this.dedupKeyTemplate = dedupKeyTemplate;
  }

  public Rule createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid
  @Schema(name = "createdAt", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public Rule updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
   */
  @NotNull @Valid
  @Schema(name = "updatedAt", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("updatedAt")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Rule rule = (Rule) o;
    return Objects.equals(this.id, rule.id) &&
        Objects.equals(this.name, rule.name) &&
        Objects.equals(this.enabled, rule.enabled) &&
        Objects.equals(this.priority, rule.priority) &&
        Objects.equals(this.source, rule.source) &&
        Objects.equals(this.conditions, rule.conditions) &&
        Objects.equals(this.severity, rule.severity) &&
        Objects.equals(this.titleTemplate, rule.titleTemplate) &&
        Objects.equals(this.descriptionTemplate, rule.descriptionTemplate) &&
        Objects.equals(this.metadataFields, rule.metadataFields) &&
        Objects.equals(this.dedupKeyTemplate, rule.dedupKeyTemplate) &&
        Objects.equals(this.createdAt, rule.createdAt) &&
        Objects.equals(this.updatedAt, rule.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, enabled, priority, source, conditions, severity, titleTemplate, descriptionTemplate, metadataFields, dedupKeyTemplate, createdAt, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Rule {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
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
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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
