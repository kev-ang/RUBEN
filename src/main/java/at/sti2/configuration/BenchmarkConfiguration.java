package at.sti2.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class BenchmarkConfiguration {

    private String name;

    private String testDataPath;

    private List<ReasoningEngineConfiguration> engines;

    private List<TestCaseConfiguration> testCases;

}