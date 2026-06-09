package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Request to assign or unassign responders.
 */

@Schema(name = "AssignIncidentRequest", description = "Request to assign or unassign responders.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class AssignIncidentRequest {

  @Valid
  private List<UUID> userIds = new ArrayList<>();

  public AssignIncidentRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AssignIncidentRequest(List<UUID> userIds) {
    this.userIds = userIds;
  }

  public AssignIncidentRequest userIds(List<UUID> userIds) {
    this.userIds = userIds;
    return this;
  }

  public AssignIncidentRequest addUserIdsItem(UUID userIdsItem) {
    if (this.userIds == null) {
      this.userIds = new ArrayList<>();
    }
    this.userIds.add(userIdsItem);
    return this;
  }

  /**
   * UUIDs of users to assign. Send empty array to clear all assignments.
   * @return userIds
   */
  @NotNull @Valid
  @Schema(name = "userIds", example = "[018e2c5f-1234-7abc-8def-0000000000aa]", description = "UUIDs of users to assign. Send empty array to clear all assignments.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("userIds")
  public List<UUID> getUserIds() {
    return userIds;
  }

  public void setUserIds(List<UUID> userIds) {
    this.userIds = userIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AssignIncidentRequest assignIncidentRequest = (AssignIncidentRequest) o;
    return Objects.equals(this.userIds, assignIncidentRequest.userIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AssignIncidentRequest {\n");
    sb.append("    userIds: ").append(toIndentedString(userIds)).append("\n");
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
