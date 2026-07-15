package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
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
 * A registered webhook source; the secret is never included.
 */

@Schema(name = "WebhookSource", description = "A registered webhook source; the secret is never included.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class WebhookSource {

  private String slug;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime secretRotatedAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastEventAt;

  public WebhookSource() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WebhookSource(String slug, OffsetDateTime createdAt) {
    this.slug = slug;
    this.createdAt = createdAt;
  }

  public WebhookSource slug(String slug) {
    this.slug = slug;
    return this;
  }

  /**
   * Path segment senders deliver to (`POST /webhooks/{slug}`).
   * @return slug
   */
  @NotNull @Pattern(regexp = "^[a-z0-9][a-z0-9_-]{0,63}$")
  @Schema(name = "slug", example = "github", description = "Path segment senders deliver to (`POST /webhooks/{slug}`).", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("slug")
  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public WebhookSource createdAt(OffsetDateTime createdAt) {
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

  public WebhookSource secretRotatedAt(@Nullable OffsetDateTime secretRotatedAt) {
    this.secretRotatedAt = secretRotatedAt;
    return this;
  }

  /**
   * Set once the secret has been rotated after creation.
   * @return secretRotatedAt
   */
  @Valid
  @Schema(name = "secretRotatedAt", description = "Set once the secret has been rotated after creation.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("secretRotatedAt")
  public @Nullable OffsetDateTime getSecretRotatedAt() {
    return secretRotatedAt;
  }

  public void setSecretRotatedAt(@Nullable OffsetDateTime secretRotatedAt) {
    this.secretRotatedAt = secretRotatedAt;
  }

  public WebhookSource lastEventAt(@Nullable OffsetDateTime lastEventAt) {
    this.lastEventAt = lastEventAt;
    return this;
  }

  /**
   * Receipt time of the newest external event for this slug; absent if none arrived yet.
   * @return lastEventAt
   */
  @Valid
  @Schema(name = "lastEventAt", description = "Receipt time of the newest external event for this slug; absent if none arrived yet.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastEventAt")
  public @Nullable OffsetDateTime getLastEventAt() {
    return lastEventAt;
  }

  public void setLastEventAt(@Nullable OffsetDateTime lastEventAt) {
    this.lastEventAt = lastEventAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebhookSource webhookSource = (WebhookSource) o;
    return Objects.equals(this.slug, webhookSource.slug) &&
        Objects.equals(this.createdAt, webhookSource.createdAt) &&
        Objects.equals(this.secretRotatedAt, webhookSource.secretRotatedAt) &&
        Objects.equals(this.lastEventAt, webhookSource.lastEventAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(slug, createdAt, secretRotatedAt, lastEventAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WebhookSource {\n");
    sb.append("    slug: ").append(toIndentedString(slug)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    secretRotatedAt: ").append(toIndentedString(secretRotatedAt)).append("\n");
    sb.append("    lastEventAt: ").append(toIndentedString(lastEventAt)).append("\n");
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
