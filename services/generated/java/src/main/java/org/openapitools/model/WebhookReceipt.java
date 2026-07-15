package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;
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
 * Acknowledgement returned by the webhook ingest endpoint.
 */

@Schema(name = "WebhookReceipt", description = "Acknowledgement returned by the webhook ingest endpoint.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class WebhookReceipt {

  private UUID id;

  private String source;

  private String eventType;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime receivedAt;

  private Boolean duplicate;

  public WebhookReceipt() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WebhookReceipt(UUID id, String source, String eventType, OffsetDateTime receivedAt, Boolean duplicate) {
    this.id = id;
    this.source = source;
    this.eventType = eventType;
    this.receivedAt = receivedAt;
    this.duplicate = duplicate;
  }

  public WebhookReceipt id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * External Event id; the `sourceId` on the resulting NATS event.
   * @return id
   */
  @NotNull @Valid
  @Schema(name = "id", example = "018e2c5f-1234-7abc-8def-0000000000e1", description = "External Event id; the `sourceId` on the resulting NATS event.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public WebhookReceipt source(String source) {
    this.source = source;
    return this;
  }

  /**
   * Get source
   * @return source
   */
  @NotNull
  @Schema(name = "source", example = "github", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("source")
  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public WebhookReceipt eventType(String eventType) {
    this.eventType = eventType;
    return this;
  }

  /**
   * Normalised event type Rules are evaluated against.
   * @return eventType
   */
  @NotNull
  @Schema(name = "eventType", example = "ci_failure", description = "Normalised event type Rules are evaluated against.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("eventType")
  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public WebhookReceipt receivedAt(OffsetDateTime receivedAt) {
    this.receivedAt = receivedAt;
    return this;
  }

  /**
   * Get receivedAt
   * @return receivedAt
   */
  @NotNull @Valid
  @Schema(name = "receivedAt", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("receivedAt")
  public OffsetDateTime getReceivedAt() {
    return receivedAt;
  }

  public void setReceivedAt(OffsetDateTime receivedAt) {
    this.receivedAt = receivedAt;
  }

  public WebhookReceipt duplicate(Boolean duplicate) {
    this.duplicate = duplicate;
    return this;
  }

  /**
   * True when a redelivery matched an already-stored event (not re-published).
   * @return duplicate
   */
  @NotNull
  @Schema(name = "duplicate", example = "false", description = "True when a redelivery matched an already-stored event (not re-published).", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("duplicate")
  public Boolean getDuplicate() {
    return duplicate;
  }

  public void setDuplicate(Boolean duplicate) {
    this.duplicate = duplicate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebhookReceipt webhookReceipt = (WebhookReceipt) o;
    return Objects.equals(this.id, webhookReceipt.id) &&
        Objects.equals(this.source, webhookReceipt.source) &&
        Objects.equals(this.eventType, webhookReceipt.eventType) &&
        Objects.equals(this.receivedAt, webhookReceipt.receivedAt) &&
        Objects.equals(this.duplicate, webhookReceipt.duplicate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, source, eventType, receivedAt, duplicate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WebhookReceipt {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
    sb.append("    eventType: ").append(toIndentedString(eventType)).append("\n");
    sb.append("    receivedAt: ").append(toIndentedString(receivedAt)).append("\n");
    sb.append("    duplicate: ").append(toIndentedString(duplicate)).append("\n");
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
