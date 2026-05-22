package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Confirmation that an AI generation task was accepted for async processing.
 */

@Schema(name = "RegenAccepted", description = "Confirmation that an AI generation task was accepted for async processing.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class RegenAccepted {

  private Boolean accepted;

  /**
   * Gets or Sets task
   */
  public enum TaskEnum {
    SUMMARY("SUMMARY"),

    SEVERITY_SUGGESTION("SEVERITY_SUGGESTION"),

    SOLUTION_SUGGESTIONS("SOLUTION_SUGGESTIONS"),

    POSTMORTEM("POSTMORTEM");

    private final String value;

    TaskEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static TaskEnum fromValue(String value) {
      for (TaskEnum b : TaskEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private TaskEnum task;

  public RegenAccepted() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RegenAccepted(Boolean accepted, TaskEnum task) {
    this.accepted = accepted;
    this.task = task;
  }

  public RegenAccepted accepted(Boolean accepted) {
    this.accepted = accepted;
    return this;
  }

  /**
   * Get accepted
   * @return accepted
   */
  @NotNull
  @Schema(name = "accepted", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("accepted")
  public Boolean getAccepted() {
    return accepted;
  }

  public void setAccepted(Boolean accepted) {
    this.accepted = accepted;
  }

  public RegenAccepted task(TaskEnum task) {
    this.task = task;
    return this;
  }

  /**
   * Get task
   * @return task
   */
  @NotNull
  @Schema(name = "task", example = "SUMMARY", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("task")
  public TaskEnum getTask() {
    return task;
  }

  public void setTask(TaskEnum task) {
    this.task = task;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RegenAccepted regenAccepted = (RegenAccepted) o;
    return Objects.equals(this.accepted, regenAccepted.accepted) &&
        Objects.equals(this.task, regenAccepted.task);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accepted, task);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RegenAccepted {\n");
    sb.append("    accepted: ").append(toIndentedString(accepted)).append("\n");
    sb.append("    task: ").append(toIndentedString(task)).append("\n");
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
