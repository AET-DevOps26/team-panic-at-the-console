package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.model.User;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * UserListResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class UserListResponse {

  @Valid
  private List<@Valid User> items = new ArrayList<>();

  private Integer total;

  private Integer limit;

  private Integer offset;

  public UserListResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UserListResponse(List<@Valid User> items, Integer total, Integer limit, Integer offset) {
    this.items = items;
    this.total = total;
    this.limit = limit;
    this.offset = offset;
  }

  public UserListResponse items(List<@Valid User> items) {
    this.items = items;
    return this;
  }

  public UserListResponse addItemsItem(User itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * Get items
   * @return items
   */
  @NotNull @Valid
  @Schema(name = "items", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("items")
  public List<@Valid User> getItems() {
    return items;
  }

  public void setItems(List<@Valid User> items) {
    this.items = items;
  }

  public UserListResponse total(Integer total) {
    this.total = total;
    return this;
  }

  /**
   * Total users matching the query (ignoring pagination).
   * minimum: 0
   * @return total
   */
  @NotNull @Min(0)
  @Schema(name = "total", example = "2", description = "Total users matching the query (ignoring pagination).", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("total")
  public Integer getTotal() {
    return total;
  }

  public void setTotal(Integer total) {
    this.total = total;
  }

  public UserListResponse limit(Integer limit) {
    this.limit = limit;
    return this;
  }

  /**
   * Get limit
   * minimum: 1
   * maximum: 100
   * @return limit
   */
  @NotNull @Min(1) @Max(100)
  @Schema(name = "limit", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("limit")
  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  public UserListResponse offset(Integer offset) {
    this.offset = offset;
    return this;
  }

  /**
   * Get offset
   * minimum: 0
   * @return offset
   */
  @NotNull @Min(0)
  @Schema(name = "offset", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("offset")
  public Integer getOffset() {
    return offset;
  }

  public void setOffset(Integer offset) {
    this.offset = offset;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserListResponse userListResponse = (UserListResponse) o;
    return Objects.equals(this.items, userListResponse.items) &&
        Objects.equals(this.total, userListResponse.total) &&
        Objects.equals(this.limit, userListResponse.limit) &&
        Objects.equals(this.offset, userListResponse.offset);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, total, limit, offset);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserListResponse {\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
    sb.append("    limit: ").append(toIndentedString(limit)).append("\n");
    sb.append("    offset: ").append(toIndentedString(offset)).append("\n");
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
