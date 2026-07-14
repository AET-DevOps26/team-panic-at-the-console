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

import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
/**
 * Single external event including the verbatim raw payload.
 */

@Schema(name = "ExternalEventDetail", description = "Single external event including the verbatim raw payload.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class ExternalEventDetail {

  private UUID id;

  private String source;

  private String eventType;

  private @Nullable String deliveryId;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime receivedAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime publishedAt;

  private Object rawPayload;

  public ExternalEventDetail() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ExternalEventDetail(UUID id, String source, String eventType, OffsetDateTime receivedAt, Object rawPayload) {
    this.id = id;
    this.source = source;
    this.eventType = eventType;
    this.receivedAt = receivedAt;
    this.rawPayload = rawPayload;
  }

  public ExternalEventDetail id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull @Valid
  @Schema(name = "id", example = "018e2c5f-1234-7abc-8def-0000000000e1", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ExternalEventDetail source(String source) {
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

  public ExternalEventDetail eventType(String eventType) {
    this.eventType = eventType;
    return this;
  }

  /**
   * Get eventType
   * @return eventType
   */
  @NotNull
  @Schema(name = "eventType", example = "ci_failure", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("eventType")
  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public ExternalEventDetail deliveryId(@Nullable String deliveryId) {
    this.deliveryId = deliveryId;
    return this;
  }

  /**
   * Sender-supplied delivery id (e.g. X-GitHub-Delivery), used for dedup.
   * @return deliveryId
   */

  @Schema(name = "deliveryId", description = "Sender-supplied delivery id (e.g. X-GitHub-Delivery), used for dedup.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("deliveryId")
  public @Nullable String getDeliveryId() {
    return deliveryId;
  }

  public void setDeliveryId(@Nullable String deliveryId) {
    this.deliveryId = deliveryId;
  }

  public ExternalEventDetail receivedAt(OffsetDateTime receivedAt) {
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

  public ExternalEventDetail publishedAt(@Nullable OffsetDateTime publishedAt) {
    this.publishedAt = publishedAt;
    return this;
  }

  /**
   * Set once the NATS publish succeeded; absent while the publish is pending.
   * @return publishedAt
   */
  @Valid
  @Schema(name = "publishedAt", description = "Set once the NATS publish succeeded; absent while the publish is pending.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("publishedAt")
  public @Nullable OffsetDateTime getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(@Nullable OffsetDateTime publishedAt) {
    this.publishedAt = publishedAt;
  }

  public ExternalEventDetail rawPayload(Object rawPayload) {
    this.rawPayload = rawPayload;
    return this;
  }

  /**
   * The original webhook payload, preserved verbatim.
   * @return rawPayload
   */
  @NotNull
  @Schema(name = "rawPayload", description = "The original webhook payload, preserved verbatim.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("rawPayload")
  public Object getRawPayload() {
    return rawPayload;
  }

  public void setRawPayload(Object rawPayload) {
    this.rawPayload = rawPayload;
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
    public ExternalEventDetail putAdditionalProperty(String key, Object value) {
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
    ExternalEventDetail externalEventDetail = (ExternalEventDetail) o;
    return Objects.equals(this.id, externalEventDetail.id) &&
        Objects.equals(this.source, externalEventDetail.source) &&
        Objects.equals(this.eventType, externalEventDetail.eventType) &&
        Objects.equals(this.deliveryId, externalEventDetail.deliveryId) &&
        Objects.equals(this.receivedAt, externalEventDetail.receivedAt) &&
        Objects.equals(this.publishedAt, externalEventDetail.publishedAt) &&
        Objects.equals(this.rawPayload, externalEventDetail.rawPayload) &&
    Objects.equals(this.additionalProperties, externalEventDetail.additionalProperties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, source, eventType, deliveryId, receivedAt, publishedAt, rawPayload, additionalProperties);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExternalEventDetail {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
    sb.append("    eventType: ").append(toIndentedString(eventType)).append("\n");
    sb.append("    deliveryId: ").append(toIndentedString(deliveryId)).append("\n");
    sb.append("    receivedAt: ").append(toIndentedString(receivedAt)).append("\n");
    sb.append("    publishedAt: ").append(toIndentedString(publishedAt)).append("\n");
    sb.append("    rawPayload: ").append(toIndentedString(rawPayload)).append("\n");

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
