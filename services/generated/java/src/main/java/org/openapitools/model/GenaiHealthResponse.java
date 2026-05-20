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
 * GenAI service health status including Ollama reachability.
 */

@Schema(name = "GenaiHealthResponse", description = "GenAI service health status including Ollama reachability.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-05-20T11:29:59.407720+02:00[Europe/Berlin]", comments = "Generator version: 7.14.0")
public class GenaiHealthResponse {

  private String status;

  private Boolean ollamaReachable;

  private String model;

  public GenaiHealthResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GenaiHealthResponse(String status, Boolean ollamaReachable, String model) {
    this.status = status;
    this.ollamaReachable = ollamaReachable;
    this.model = model;
  }

  public GenaiHealthResponse status(String status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull
  @Schema(name = "status", example = "ok", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public GenaiHealthResponse ollamaReachable(Boolean ollamaReachable) {
    this.ollamaReachable = ollamaReachable;
    return this;
  }

  /**
   * Get ollamaReachable
   * @return ollamaReachable
   */
  @NotNull
  @Schema(name = "ollamaReachable", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("ollamaReachable")
  public Boolean getOllamaReachable() {
    return ollamaReachable;
  }

  public void setOllamaReachable(Boolean ollamaReachable) {
    this.ollamaReachable = ollamaReachable;
  }

  public GenaiHealthResponse model(String model) {
    this.model = model;
    return this;
  }

  /**
   * Get model
   * @return model
   */
  @NotNull
  @Schema(name = "model", example = "qwen2.5:3b", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("model")
  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GenaiHealthResponse genaiHealthResponse = (GenaiHealthResponse) o;
    return Objects.equals(this.status, genaiHealthResponse.status) &&
        Objects.equals(this.ollamaReachable, genaiHealthResponse.ollamaReachable) &&
        Objects.equals(this.model, genaiHealthResponse.model);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, ollamaReachable, model);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GenaiHealthResponse {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    ollamaReachable: ").append(toIndentedString(ollamaReachable)).append("\n");
    sb.append("    model: ").append(toIndentedString(model)).append("\n");
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
