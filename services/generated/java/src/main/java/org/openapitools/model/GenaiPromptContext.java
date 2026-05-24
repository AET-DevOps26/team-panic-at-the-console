package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.model.Event;
import org.openapitools.model.Incident;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
/**
 * Incident snapshot and event log used when assembling GenAI prompts.
 */

@Schema(name = "GenaiPromptContext", description = "Incident snapshot and event log used when assembling GenAI prompts.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class GenaiPromptContext {

  private Incident incident;

  @Valid
  private List<@Valid Event> events = new ArrayList<>();

  public GenaiPromptContext() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GenaiPromptContext(Incident incident, List<@Valid Event> events) {
    this.incident = incident;
    this.events = events;
  }

  public GenaiPromptContext incident(Incident incident) {
    this.incident = incident;
    return this;
  }

  /**
   * Get incident
   * @return incident
   */
  @NotNull @Valid
  @Schema(name = "incident", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("incident")
  public Incident getIncident() {
    return incident;
  }

  public void setIncident(Incident incident) {
    this.incident = incident;
  }

  public GenaiPromptContext events(List<@Valid Event> events) {
    this.events = events;
    return this;
  }

  public GenaiPromptContext addEventsItem(Event eventsItem) {
    if (this.events == null) {
      this.events = new ArrayList<>();
    }
    this.events.add(eventsItem);
    return this;
  }

  /**
   * Get events
   * @return events
   */
  @NotNull @Valid
  @Schema(name = "events", example = "[{timestamp=2026-05-24T14:18:00Z, type=severity_changed, description=Severity escalated from SEV3 to SEV2}]", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("events")
  public List<@Valid Event> getEvents() {
    return events;
  }

  public void setEvents(List<@Valid Event> events) {
    this.events = events;
  }
    /**
    * A container for additional, undeclared properties.
    * This is a holder for any undeclared properties as specified with
    * the 'additionalProperties' keyword in the OAS document.
    */
    private Map<String, Object> additionalProperties;

    /**
    * Set the additional (undeclared) property with the specified name and value.
    * If the property does not already exist, create it otherwise replace it.
    */
    @JsonAnySetter
    public GenaiPromptContext putAdditionalProperty(String key, Object value) {
        if (this.additionalProperties == null) {
            this.additionalProperties = new HashMap<String, Object>();
        }
        this.additionalProperties.put(key, value);
        return this;
    }

    /**
    * Return the additional (undeclared) property.
    */
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    /**
    * Return the additional (undeclared) property with the specified name.
    */
    public Object getAdditionalProperty(String key) {
        if (this.additionalProperties == null) {
            return null;
        }
        return this.additionalProperties.get(key);
    }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GenaiPromptContext genaiPromptContext = (GenaiPromptContext) o;
    return Objects.equals(this.incident, genaiPromptContext.incident) &&
        Objects.equals(this.events, genaiPromptContext.events) &&
    Objects.equals(this.additionalProperties, genaiPromptContext.additionalProperties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(incident, events, additionalProperties);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GenaiPromptContext {\n");
    sb.append("    incident: ").append(toIndentedString(incident)).append("\n");
    sb.append("    events: ").append(toIndentedString(events)).append("\n");

    sb.append("    additionalProperties: ").append(toIndentedString(additionalProperties)).append("\n");
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
