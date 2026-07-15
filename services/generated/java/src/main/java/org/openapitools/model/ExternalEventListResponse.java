package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.model.ExternalEventSummary;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Page of external events, newest first.
 */

@Schema(name = "ExternalEventListResponse", description = "Page of external events, newest first.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class ExternalEventListResponse {

  @Valid
  private List<@Valid ExternalEventSummary> items = new ArrayList<>();

  private Integer total;

  private Integer page;

  private Integer size;

  public ExternalEventListResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ExternalEventListResponse(List<@Valid ExternalEventSummary> items, Integer total, Integer page, Integer size) {
    this.items = items;
    this.total = total;
    this.page = page;
    this.size = size;
  }

  public ExternalEventListResponse items(List<@Valid ExternalEventSummary> items) {
    this.items = items;
    return this;
  }

  public ExternalEventListResponse addItemsItem(ExternalEventSummary itemsItem) {
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
  public List<@Valid ExternalEventSummary> getItems() {
    return items;
  }

  public void setItems(List<@Valid ExternalEventSummary> items) {
    this.items = items;
  }

  public ExternalEventListResponse total(Integer total) {
    this.total = total;
    return this;
  }

  /**
   * Get total
   * minimum: 0
   * @return total
   */
  @NotNull @Min(0)
  @Schema(name = "total", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("total")
  public Integer getTotal() {
    return total;
  }

  public void setTotal(Integer total) {
    this.total = total;
  }

  public ExternalEventListResponse page(Integer page) {
    this.page = page;
    return this;
  }

  /**
   * Get page
   * minimum: 0
   * @return page
   */
  @NotNull @Min(0)
  @Schema(name = "page", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("page")
  public Integer getPage() {
    return page;
  }

  public void setPage(Integer page) {
    this.page = page;
  }

  public ExternalEventListResponse size(Integer size) {
    this.size = size;
    return this;
  }

  /**
   * Get size
   * minimum: 1
   * maximum: 100
   * @return size
   */
  @NotNull @Min(1) @Max(100)
  @Schema(name = "size", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("size")
  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExternalEventListResponse externalEventListResponse = (ExternalEventListResponse) o;
    return Objects.equals(this.items, externalEventListResponse.items) &&
        Objects.equals(this.total, externalEventListResponse.total) &&
        Objects.equals(this.page, externalEventListResponse.page) &&
        Objects.equals(this.size, externalEventListResponse.size);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, total, page, size);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExternalEventListResponse {\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
    sb.append("    page: ").append(toIndentedString(page)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
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
