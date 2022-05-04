package at.sti2.model.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Query {
    @JsonProperty("name")
    private String name;

    @JsonProperty("query")
    private String query;
}
