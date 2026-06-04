package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * PostmortemPatch
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class PostmortemPatch {

  private String rootCause;

  @Valid
  private List<String> timeline = new ArrayList<>();

  @Valid
  private List<String> actionItems = new ArrayList<>();

  public PostmortemPatch() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PostmortemPatch(String rootCause, List<String> timeline, List<String> actionItems) {
    this.rootCause = rootCause;
    this.timeline = timeline;
    this.actionItems = actionItems;
  }

  public PostmortemPatch rootCause(String rootCause) {
    this.rootCause = rootCause;
    return this;
  }

  /**
   * Get rootCause
   * @return rootCause
   */
  @NotNull @Size(min = 1)
  @Schema(name = "rootCause", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("rootCause")
  public String getRootCause() {
    return rootCause;
  }

  public void setRootCause(String rootCause) {
    this.rootCause = rootCause;
  }

  public PostmortemPatch timeline(List<String> timeline) {
    this.timeline = timeline;
    return this;
  }

  public PostmortemPatch addTimelineItem(String timelineItem) {
    if (this.timeline == null) {
      this.timeline = new ArrayList<>();
    }
    this.timeline.add(timelineItem);
    return this;
  }

  /**
   * Get timeline
   * @return timeline
   */
  @NotNull @Size(min = 1)
  @Schema(name = "timeline", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("timeline")
  public List<String> getTimeline() {
    return timeline;
  }

  public void setTimeline(List<String> timeline) {
    this.timeline = timeline;
  }

  public PostmortemPatch actionItems(List<String> actionItems) {
    this.actionItems = actionItems;
    return this;
  }

  public PostmortemPatch addActionItemsItem(String actionItemsItem) {
    if (this.actionItems == null) {
      this.actionItems = new ArrayList<>();
    }
    this.actionItems.add(actionItemsItem);
    return this;
  }

  /**
   * Get actionItems
   * @return actionItems
   */
  @NotNull @Size(min = 1)
  @Schema(name = "actionItems", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("actionItems")
  public List<String> getActionItems() {
    return actionItems;
  }

  public void setActionItems(List<String> actionItems) {
    this.actionItems = actionItems;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PostmortemPatch postmortemPatch = (PostmortemPatch) o;
    return Objects.equals(this.rootCause, postmortemPatch.rootCause) &&
        Objects.equals(this.timeline, postmortemPatch.timeline) &&
        Objects.equals(this.actionItems, postmortemPatch.actionItems);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rootCause, timeline, actionItems);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostmortemPatch {\n");
    sb.append("    rootCause: ").append(toIndentedString(rootCause)).append("\n");
    sb.append("    timeline: ").append(toIndentedString(timeline)).append("\n");
    sb.append("    actionItems: ").append(toIndentedString(actionItems)).append("\n");
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
