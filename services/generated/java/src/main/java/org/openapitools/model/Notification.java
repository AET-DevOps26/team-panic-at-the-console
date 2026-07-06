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
import org.openapitools.model.NotificationType;
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
 * An in-app notification about an incident event.
 */

@Schema(name = "Notification", description = "An in-app notification about an incident event.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class Notification {

  private UUID id;

  private UUID incidentId;

  private NotificationType type;

  private JsonNullable<UUID> recipientId = JsonNullable.<UUID>undefined();

  private String message;

  private Boolean read;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public Notification() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Notification(UUID id, UUID incidentId, NotificationType type, String message, Boolean read, OffsetDateTime createdAt) {
    this.id = id;
    this.incidentId = incidentId;
    this.type = type;
    this.message = message;
    this.read = read;
    this.createdAt = createdAt;
  }

  public Notification id(UUID id) {
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

  public Notification incidentId(UUID incidentId) {
    this.incidentId = incidentId;
    return this;
  }

  /**
   * Get incidentId
   * @return incidentId
   */
  @NotNull @Valid
  @Schema(name = "incidentId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("incidentId")
  public UUID getIncidentId() {
    return incidentId;
  }

  public void setIncidentId(UUID incidentId) {
    this.incidentId = incidentId;
  }

  public Notification type(NotificationType type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid
  @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public NotificationType getType() {
    return type;
  }

  public void setType(NotificationType type) {
    this.type = type;
  }

  public Notification recipientId(UUID recipientId) {
    this.recipientId = JsonNullable.of(recipientId);
    return this;
  }

  /**
   * Target user for a personal notification; null for a broadcast visible to everyone.
   * @return recipientId
   */
  @Valid
  @Schema(name = "recipientId", description = "Target user for a personal notification; null for a broadcast visible to everyone.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("recipientId")
  public JsonNullable<UUID> getRecipientId() {
    return recipientId;
  }

  public void setRecipientId(JsonNullable<UUID> recipientId) {
    this.recipientId = recipientId;
  }

  public Notification message(String message) {
    this.message = message;
    return this;
  }

  /**
   * Get message
   * @return message
   */
  @NotNull
  @Schema(name = "message", example = "You were assigned to an incident.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Notification read(Boolean read) {
    this.read = read;
    return this;
  }

  /**
   * Get read
   * @return read
   */
  @NotNull
  @Schema(name = "read", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("read")
  public Boolean getRead() {
    return read;
  }

  public void setRead(Boolean read) {
    this.read = read;
  }

  public Notification createdAt(OffsetDateTime createdAt) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Notification notification = (Notification) o;
    return Objects.equals(this.id, notification.id) &&
        Objects.equals(this.incidentId, notification.incidentId) &&
        Objects.equals(this.type, notification.type) &&
        equalsNullable(this.recipientId, notification.recipientId) &&
        Objects.equals(this.message, notification.message) &&
        Objects.equals(this.read, notification.read) &&
        Objects.equals(this.createdAt, notification.createdAt);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, incidentId, type, hashCodeNullable(recipientId), message, read, createdAt);
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
    sb.append("class Notification {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    incidentId: ").append(toIndentedString(incidentId)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    recipientId: ").append(toIndentedString(recipientId)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    read: ").append(toIndentedString(read)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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
