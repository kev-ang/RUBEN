package at.sti2.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TestCaseConfiguration {

  @JsonProperty("test_case_identifier")
  private String testCaseIdentifier;

  @JsonProperty("test_name")
  private String testName;

  @JsonProperty("category")
  private String category;

  public String getName() {
    return String.join("_", category, testName, testCaseIdentifier);
  }
}
