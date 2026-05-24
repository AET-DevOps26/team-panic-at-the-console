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
 * Latest AI-generated postmortem draft for a resolved incident.
 */

@Schema(name = "PostmortemResponse", description = "Latest AI-generated postmortem draft for a resolved incident.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class PostmortemResponse {

  private String rootCause;

  @Valid
  private List<String> timeline = new ArrayList<>();

  @Valid
  private List<String> actionItems = new ArrayList<>();

  public PostmortemResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PostmortemResponse(String rootCause, List<String> timeline, List<String> actionItems) {
    this.rootCause = rootCause;
    this.timeline = timeline;
    this.actionItems = actionItems;
  }

  public PostmortemResponse rootCause(String rootCause) {
    this.rootCause = rootCause;
    return this;
  }

  /**
   * Get rootCause
   * @return rootCause
   */
  @NotNull
  @Schema(name = "rootCause", example = "Connection pool misconfiguration in payment-service v2.4.1", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("rootCause")
  public String getRootCause() {
    return rootCause;
  }

  public void setRootCause(String rootCause) {
    this.rootCause = rootCause;
  }

  public PostmortemResponse timeline(List<String> timeline) {
    this.timeline = timeline;
    return this;
  }

  public PostmortemResponse addTimelineItem(String timelineItem) {
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
  @NotNull
  @Schema(name = "timeline", example = "[14:02 Deploy v2.4.1 completed, 14:18 Checkout error rate crossed 5%]", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("timeline")
  public List<String> getTimeline() {
    return timeline;
  }

  public void setTimeline(List<String> timeline) {
    this.timeline = timeline;
  }

  public PostmortemResponse actionItems(List<String> actionItems) {
    this.actionItems = actionItems;
    return this;
  }

  public PostmortemResponse addActionItemsItem(String actionItemsItem) {
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
  @NotNull
  @Schema(name = "actionItems", example = "[Add pool-size validation to deploy checklist, Alert on checkout latency SLO burn]", requiredMode = Schema.RequiredMode.REQUIRED)
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
    PostmortemResponse postmortemResponse = (PostmortemResponse) o;
    return Objects.equals(this.rootCause, postmortemResponse.rootCause) &&
        Objects.equals(this.timeline, postmortemResponse.timeline) &&
        Objects.equals(this.actionItems, postmortemResponse.actionItems);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rootCause, timeline, actionItems);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostmortemResponse {\n");
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
