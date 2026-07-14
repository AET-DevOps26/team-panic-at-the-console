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
 * Create/rotate response carrying the generated HMAC secret. The secret is not retrievable afterwards; the server keeps it only for signature verification.
 */

@Schema(name = "WebhookSourceWithSecret", description = "Create/rotate response carrying the generated HMAC secret. The secret is not retrievable afterwards; the server keeps it only for signature verification.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class WebhookSourceWithSecret {

  private String slug;

  private String secret;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime secretRotatedAt;

  public WebhookSourceWithSecret() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WebhookSourceWithSecret(String slug, String secret, OffsetDateTime createdAt) {
    this.slug = slug;
    this.secret = secret;
    this.createdAt = createdAt;
  }

  public WebhookSourceWithSecret slug(String slug) {
    this.slug = slug;
    return this;
  }

  /**
   * Get slug
   * @return slug
   */
  @NotNull
  @Schema(name = "slug", example = "grafana", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("slug")
  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public WebhookSourceWithSecret secret(String secret) {
    this.secret = secret;
    return this;
  }

  /**
   * Hex-encoded 256-bit secret for HMAC-SHA256 signing (`X-Hub-Signature-256`).
   * @return secret
   */
  @NotNull
  @Schema(name = "secret", example = "6bc1bee22e409f96e93d7e117393172aad4c8f10b0e6371b2b647a2f45c7c463", description = "Hex-encoded 256-bit secret for HMAC-SHA256 signing (`X-Hub-Signature-256`).", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("secret")
  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public WebhookSourceWithSecret createdAt(OffsetDateTime createdAt) {
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

  public WebhookSourceWithSecret secretRotatedAt(@Nullable OffsetDateTime secretRotatedAt) {
    this.secretRotatedAt = secretRotatedAt;
    return this;
  }

  /**
   * Set when this response comes from a rotation.
   * @return secretRotatedAt
   */
  @Valid
  @Schema(name = "secretRotatedAt", description = "Set when this response comes from a rotation.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("secretRotatedAt")
  public @Nullable OffsetDateTime getSecretRotatedAt() {
    return secretRotatedAt;
  }

  public void setSecretRotatedAt(@Nullable OffsetDateTime secretRotatedAt) {
    this.secretRotatedAt = secretRotatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebhookSourceWithSecret webhookSourceWithSecret = (WebhookSourceWithSecret) o;
    return Objects.equals(this.slug, webhookSourceWithSecret.slug) &&
        Objects.equals(this.secret, webhookSourceWithSecret.secret) &&
        Objects.equals(this.createdAt, webhookSourceWithSecret.createdAt) &&
        Objects.equals(this.secretRotatedAt, webhookSourceWithSecret.secretRotatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(slug, secret, createdAt, secretRotatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WebhookSourceWithSecret {\n");
    sb.append("    slug: ").append(toIndentedString(slug)).append("\n");
    sb.append("    secret: ").append(toIndentedString(secret)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    secretRotatedAt: ").append(toIndentedString(secretRotatedAt)).append("\n");
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
