package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.model.Incident;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * IncidentListResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class IncidentListResponse {

  @Valid
  private List<@Valid Incident> items = new ArrayList<>();

  private Integer total;

  private Integer limit;

  private Integer offset;

  public IncidentListResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IncidentListResponse(List<@Valid Incident> items, Integer total, Integer limit, Integer offset) {
    this.items = items;
    this.total = total;
    this.limit = limit;
    this.offset = offset;
  }

  public IncidentListResponse items(List<@Valid Incident> items) {
    this.items = items;
    return this;
  }

  public IncidentListResponse addItemsItem(Incident itemsItem) {
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
  public List<@Valid Incident> getItems() {
    return items;
  }

  public void setItems(List<@Valid Incident> items) {
    this.items = items;
  }

  public IncidentListResponse total(Integer total) {
    this.total = total;
    return this;
  }

  /**
   * Get total
   * minimum: 0
   * @return total
   */
  @NotNull @Min(0)
  @Schema(name = "total", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("total")
  public Integer getTotal() {
    return total;
  }

  public void setTotal(Integer total) {
    this.total = total;
  }

  public IncidentListResponse limit(Integer limit) {
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

  public IncidentListResponse offset(Integer offset) {
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
    IncidentListResponse incidentListResponse = (IncidentListResponse) o;
    return Objects.equals(this.items, incidentListResponse.items) &&
        Objects.equals(this.total, incidentListResponse.total) &&
        Objects.equals(this.limit, incidentListResponse.limit) &&
        Objects.equals(this.offset, incidentListResponse.offset);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, total, limit, offset);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IncidentListResponse {\n");
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
