package at.sti2.configuration;

import java.util.Map;
import lombok.Data;

@Data
public class RuleEngineConfiguration {

    private String name;

    private String classpath;

    private Map<String, Object> settings;

}