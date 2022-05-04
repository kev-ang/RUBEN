package at.sti2.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Data;

@Data
public class ReasoningEngineConfiguration {

    @JsonProperty("name")
    private String name;

    @JsonProperty("classpath")
    private String classpath;

    @JsonProperty("settings")
    private Map<String, Object> settings;

}
