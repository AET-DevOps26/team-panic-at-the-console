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
 * SolutionsPatch
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-05-20T11:29:59.407720+02:00[Europe/Berlin]", comments = "Generator version: 7.14.0")
public class SolutionsPatch {

  @Valid
  private List<@Size(min = 1)String> solutions = new ArrayList<>();

  public SolutionsPatch() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SolutionsPatch(List<@Size(min = 1)String> solutions) {
    this.solutions = solutions;
  }

  public SolutionsPatch solutions(List<@Size(min = 1)String> solutions) {
    this.solutions = solutions;
    return this;
  }

  public SolutionsPatch addSolutionsItem(String solutionsItem) {
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
  @NotNull @Size(min = 1)
  @Schema(name = "solutions", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("solutions")
  public List<@Size(min = 1)String> getSolutions() {
    return solutions;
  }

  public void setSolutions(List<@Size(min = 1)String> solutions) {
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
    SolutionsPatch solutionsPatch = (SolutionsPatch) o;
    return Objects.equals(this.solutions, solutionsPatch.solutions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(solutions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SolutionsPatch {\n");
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
