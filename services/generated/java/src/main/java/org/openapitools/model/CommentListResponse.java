package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.model.Comment;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CommentListResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class CommentListResponse {

  @Valid
  private List<@Valid Comment> items = new ArrayList<>();

  private Integer total;

  private Integer limit;

  private Integer offset;

  public CommentListResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CommentListResponse(List<@Valid Comment> items, Integer total, Integer limit, Integer offset) {
    this.items = items;
    this.total = total;
    this.limit = limit;
    this.offset = offset;
  }

  public CommentListResponse items(List<@Valid Comment> items) {
    this.items = items;
    return this;
  }

  public CommentListResponse addItemsItem(Comment itemsItem) {
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
  public List<@Valid Comment> getItems() {
    return items;
  }

  public void setItems(List<@Valid Comment> items) {
    this.items = items;
  }

  public CommentListResponse total(Integer total) {
    this.total = total;
    return this;
  }

  /**
   * Get total
   * minimum: 0
   * @return total
   */
  @NotNull @Min(0)
  @Schema(name = "total", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("total")
  public Integer getTotal() {
    return total;
  }

  public void setTotal(Integer total) {
    this.total = total;
  }

  public CommentListResponse limit(Integer limit) {
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

  public CommentListResponse offset(Integer offset) {
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
    CommentListResponse commentListResponse = (CommentListResponse) o;
    return Objects.equals(this.items, commentListResponse.items) &&
        Objects.equals(this.total, commentListResponse.total) &&
        Objects.equals(this.limit, commentListResponse.limit) &&
        Objects.equals(this.offset, commentListResponse.offset);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, total, limit, offset);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CommentListResponse {\n");
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
