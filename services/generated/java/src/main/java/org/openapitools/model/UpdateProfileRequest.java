package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Partial profile update; at least one of &#x60;email&#x60; or &#x60;displayName&#x60; must be present. &#x60;currentPassword&#x60; is required when &#x60;email&#x60; is present.
 */

@Schema(name = "UpdateProfileRequest", description = "Partial profile update; at least one of `email` or `displayName` must be present. `currentPassword` is required when `email` is present.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class UpdateProfileRequest {

  private @Nullable String email;

  private @Nullable String displayName;

  private @Nullable String currentPassword;

  public UpdateProfileRequest email(@Nullable String email) {
    this.email = email;
    return this;
  }

  /**
   * Get email
   * @return email
   */
  @jakarta.validation.constraints.Email
  @Schema(name = "email", example = "new.address@example.com", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("email")
  public @Nullable String getEmail() {
    return email;
  }

  public void setEmail(@Nullable String email) {
    this.email = email;
  }

  public UpdateProfileRequest displayName(@Nullable String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Get displayName
   * @return displayName
   */
  @Size(min = 1, max = 100)
  @Schema(name = "displayName", example = "Alex Responder", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("displayName")
  public @Nullable String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(@Nullable String displayName) {
    this.displayName = displayName;
  }

  public UpdateProfileRequest currentPassword(@Nullable String currentPassword) {
    this.currentPassword = currentPassword;
    return this;
  }

  /**
   * Get currentPassword
   * @return currentPassword
   */

  @Schema(name = "currentPassword", example = "change-me-8+", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("currentPassword")
  public @Nullable String getCurrentPassword() {
    return currentPassword;
  }

  public void setCurrentPassword(@Nullable String currentPassword) {
    this.currentPassword = currentPassword;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateProfileRequest updateProfileRequest = (UpdateProfileRequest) o;
    return Objects.equals(this.email, updateProfileRequest.email) &&
        Objects.equals(this.displayName, updateProfileRequest.displayName) &&
        Objects.equals(this.currentPassword, updateProfileRequest.currentPassword);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, displayName, currentPassword);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateProfileRequest {\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    currentPassword: ").append("*").append("\n");
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
