package at.sti2.configuration;

import java.util.List;
import lombok.Data;

@Data
public class BenchmarkConfiguration {

    private String name;

    private String testDataPath;

    private List<RuleEngineConfiguration> engines;

    private List<TestCaseConfiguration> testCases;

}