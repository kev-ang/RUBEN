package at.sti2.model.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class QueryContainer {

    @JsonProperty("queries")
    private List<Query> queries;
}
