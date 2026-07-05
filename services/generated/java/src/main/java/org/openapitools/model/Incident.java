package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;
import org.openapitools.model.IncidentStatus;
import org.openapitools.model.Severity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * An incident as stored by incident-service.
 */

@Schema(name = "Incident", description = "An incident as stored by incident-service.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class Incident {

  private UUID id;

  private String title;

  private JsonNullable<String> description = JsonNullable.<String>undefined();

  private IncidentStatus status;

  private Severity severity;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> resolvedAt = JsonNullable.<OffsetDateTime>undefined();

  private JsonNullable<String> summary = JsonNullable.<String>undefined();

  private JsonNullable<String> severitySuggestion = JsonNullable.<String>undefined();

  private JsonNullable<String> solutions = JsonNullable.<String>undefined();

  private JsonNullable<String> postmortem = JsonNullable.<String>undefined();

  public Incident() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Incident(UUID id, String title, IncidentStatus status, Severity severity, OffsetDateTime createdAt) {
    this.id = id;
    this.title = title;
    this.status = status;
    this.severity = severity;
    this.createdAt = createdAt;
  }

  public Incident id(UUID id) {
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

  public Incident title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Get title
   * @return title
   */
  @NotNull
  @Schema(name = "title", example = "Checkout 5xx spike", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Incident description(String description) {
    this.description = JsonNullable.of(description);
    return this;
  }

  /**
   * Get description
   * @return description
   */

  @Schema(name = "description", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public JsonNullable<String> getDescription() {
    return description;
  }

  public void setDescription(JsonNullable<String> description) {
    this.description = description;
  }

  public Incident status(IncidentStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid
  @Schema(name = "status", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public IncidentStatus getStatus() {
    return status;
  }

  public void setStatus(IncidentStatus status) {
    this.status = status;
  }

  public Incident severity(Severity severity) {
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

  public Incident createdAt(OffsetDateTime createdAt) {
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

  public Incident resolvedAt(OffsetDateTime resolvedAt) {
    this.resolvedAt = JsonNullable.of(resolvedAt);
    return this;
  }

  /**
   * Get resolvedAt
   * @return resolvedAt
   */
  @Valid
  @Schema(name = "resolvedAt", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("resolvedAt")
  public JsonNullable<OffsetDateTime> getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(JsonNullable<OffsetDateTime> resolvedAt) {
    this.resolvedAt = resolvedAt;
  }

  public Incident summary(String summary) {
    this.summary = JsonNullable.of(summary);
    return this;
  }

  /**
   * AI-generated narrative summary. Regenerable on demand.
   * @return summary
   */

  @Schema(name = "summary", description = "AI-generated narrative summary. Regenerable on demand.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("summary")
  public JsonNullable<String> getSummary() {
    return summary;
  }

  public void setSummary(JsonNullable<String> summary) {
    this.summary = summary;
  }

  public Incident severitySuggestion(String severitySuggestion) {
    this.severitySuggestion = JsonNullable.of(severitySuggestion);
    return this;
  }

  /**
   * AI-suggested severity with reasoning, formatted as \"SEV<n>: <reason>\".
   * @return severitySuggestion
   */

  @Schema(name = "severitySuggestion", description = "AI-suggested severity with reasoning, formatted as \"SEV<n>: <reason>\".", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("severitySuggestion")
  public JsonNullable<String> getSeveritySuggestion() {
    return severitySuggestion;
  }

  public void setSeveritySuggestion(JsonNullable<String> severitySuggestion) {
    this.severitySuggestion = severitySuggestion;
  }

  public Incident solutions(String solutions) {
    this.solutions = JsonNullable.of(solutions);
    return this;
  }

  /**
   * AI-suggested remediation steps, one per line.
   * @return solutions
   */

  @Schema(name = "solutions", description = "AI-suggested remediation steps, one per line.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("solutions")
  public JsonNullable<String> getSolutions() {
    return solutions;
  }

  public void setSolutions(JsonNullable<String> solutions) {
    this.solutions = solutions;
  }

  public Incident postmortem(String postmortem) {
    this.postmortem = JsonNullable.of(postmortem);
    return this;
  }

  /**
   * AI-drafted postmortem. Only set for resolved incidents.
   * @return postmortem
   */

  @Schema(name = "postmortem", description = "AI-drafted postmortem. Only set for resolved incidents.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("postmortem")
  public JsonNullable<String> getPostmortem() {
    return postmortem;
  }

  public void setPostmortem(JsonNullable<String> postmortem) {
    this.postmortem = postmortem;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Incident incident = (Incident) o;
    return Objects.equals(this.id, incident.id) &&
        Objects.equals(this.title, incident.title) &&
        equalsNullable(this.description, incident.description) &&
        Objects.equals(this.status, incident.status) &&
        Objects.equals(this.severity, incident.severity) &&
        Objects.equals(this.createdAt, incident.createdAt) &&
        equalsNullable(this.resolvedAt, incident.resolvedAt) &&
        equalsNullable(this.summary, incident.summary) &&
        equalsNullable(this.severitySuggestion, incident.severitySuggestion) &&
        equalsNullable(this.solutions, incident.solutions) &&
        equalsNullable(this.postmortem, incident.postmortem);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title, hashCodeNullable(description), status, severity, createdAt, hashCodeNullable(resolvedAt), hashCodeNullable(summary), hashCodeNullable(severitySuggestion), hashCodeNullable(solutions), hashCodeNullable(postmortem));
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Incident {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    severity: ").append(toIndentedString(severity)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    resolvedAt: ").append(toIndentedString(resolvedAt)).append("\n");
    sb.append("    summary: ").append(toIndentedString(summary)).append("\n");
    sb.append("    severitySuggestion: ").append(toIndentedString(severitySuggestion)).append("\n");
    sb.append("    solutions: ").append(toIndentedString(solutions)).append("\n");
    sb.append("    postmortem: ").append(toIndentedString(postmortem)).append("\n");
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
