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
 * An immutable comment left on an incident.
 */

@Schema(name = "Comment", description = "An immutable comment left on an incident.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class Comment {

  private UUID id;

  private UUID incidentId;

  private UUID authorId;

  private String content;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public Comment() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Comment(UUID id, UUID incidentId, UUID authorId, String content, OffsetDateTime createdAt) {
    this.id = id;
    this.incidentId = incidentId;
    this.authorId = authorId;
    this.content = content;
    this.createdAt = createdAt;
  }

  public Comment id(UUID id) {
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

  public Comment incidentId(UUID incidentId) {
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

  public Comment authorId(UUID authorId) {
    this.authorId = authorId;
    return this;
  }

  /**
   * Get authorId
   * @return authorId
   */
  @NotNull @Valid
  @Schema(name = "authorId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("authorId")
  public UUID getAuthorId() {
    return authorId;
  }

  public void setAuthorId(UUID authorId) {
    this.authorId = authorId;
  }

  public Comment content(String content) {
    this.content = content;
    return this;
  }

  /**
   * Get content
   * @return content
   */
  @NotNull
  @Schema(name = "content", example = "Possible root cause is the connection pool configuration in v2.4.1", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("content")
  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Comment createdAt(OffsetDateTime createdAt) {
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
    Comment comment = (Comment) o;
    return Objects.equals(this.id, comment.id) &&
        Objects.equals(this.incidentId, comment.incidentId) &&
        Objects.equals(this.authorId, comment.authorId) &&
        Objects.equals(this.content, comment.content) &&
        Objects.equals(this.createdAt, comment.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, incidentId, authorId, content, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Comment {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    incidentId: ").append(toIndentedString(incidentId)).append("\n");
    sb.append("    authorId: ").append(toIndentedString(authorId)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
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
