package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.model.Notification;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * NotificationListResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class NotificationListResponse {

  @Valid
  private List<@Valid Notification> items = new ArrayList<>();

  private Integer total;

  private Integer page;

  private Integer size;

  private Integer unreadCount;

  public NotificationListResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NotificationListResponse(List<@Valid Notification> items, Integer total, Integer page, Integer size, Integer unreadCount) {
    this.items = items;
    this.total = total;
    this.page = page;
    this.size = size;
    this.unreadCount = unreadCount;
  }

  public NotificationListResponse items(List<@Valid Notification> items) {
    this.items = items;
    return this;
  }

  public NotificationListResponse addItemsItem(Notification itemsItem) {
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
  public List<@Valid Notification> getItems() {
    return items;
  }

  public void setItems(List<@Valid Notification> items) {
    this.items = items;
  }

  public NotificationListResponse total(Integer total) {
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

  public NotificationListResponse page(Integer page) {
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

  public NotificationListResponse size(Integer size) {
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

  public NotificationListResponse unreadCount(Integer unreadCount) {
    this.unreadCount = unreadCount;
    return this;
  }

  /**
   * Number of unread notifications for the calling user (independent of pagination).
   * minimum: 0
   * @return unreadCount
   */
  @NotNull @Min(0)
  @Schema(name = "unreadCount", example = "2", description = "Number of unread notifications for the calling user (independent of pagination).", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("unreadCount")
  public Integer getUnreadCount() {
    return unreadCount;
  }

  public void setUnreadCount(Integer unreadCount) {
    this.unreadCount = unreadCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationListResponse notificationListResponse = (NotificationListResponse) o;
    return Objects.equals(this.items, notificationListResponse.items) &&
        Objects.equals(this.total, notificationListResponse.total) &&
        Objects.equals(this.page, notificationListResponse.page) &&
        Objects.equals(this.size, notificationListResponse.size) &&
        Objects.equals(this.unreadCount, notificationListResponse.unreadCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, total, page, size, unreadCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationListResponse {\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
    sb.append("    page: ").append(toIndentedString(page)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    unreadCount: ").append(toIndentedString(unreadCount)).append("\n");
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
