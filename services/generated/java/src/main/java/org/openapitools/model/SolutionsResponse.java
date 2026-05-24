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
 * Latest AI-generated remediation suggestions for an incident.
 */

@Schema(name = "SolutionsResponse", description = "Latest AI-generated remediation suggestions for an incident.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class SolutionsResponse {

  @Valid
  private List<String> solutions = new ArrayList<>();

  public SolutionsResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SolutionsResponse(List<String> solutions) {
    this.solutions = solutions;
  }

  public SolutionsResponse solutions(List<String> solutions) {
    this.solutions = solutions;
    return this;
  }

  public SolutionsResponse addSolutionsItem(String solutionsItem) {
    if (this.solutions == null) {
      this.solutions = new ArrayList<>();
    }
    this.solutions.add(solutionsItem);
    return this;
  }

  /**
   * Get solutions
   * @return solutions
   */
  @NotNull
  @Schema(name = "solutions", example = "[Roll back payment-service to v2.3.9, Scale checkout-api replicas to 6]", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("solutions")
  public List<String> getSolutions() {
    return solutions;
  }

  public void setSolutions(List<String> solutions) {
    this.solutions = solutions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SolutionsResponse solutionsResponse = (SolutionsResponse) o;
    return Objects.equals(this.solutions, solutionsResponse.solutions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(solutions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SolutionsResponse {\n");
    sb.append("    solutions: ").append(toIndentedString(solutions)).append("\n");
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
