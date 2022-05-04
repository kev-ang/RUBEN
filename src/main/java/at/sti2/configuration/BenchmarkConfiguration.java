package at.sti2.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class BenchmarkConfiguration {

    @JsonProperty("name")
    private String name;

    @JsonProperty("engines")
    private List<ReasoningEngineConfiguration> benchmarkEngines;

    @JsonProperty("test_cases")
    private List<TestCaseConfiguration> testCases;

}
