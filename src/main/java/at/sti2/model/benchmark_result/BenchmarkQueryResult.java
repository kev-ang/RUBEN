package at.sti2.model.benchmark_result;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class BenchmarkQueryResult {

    private String query;
    private long timeSpent;
    private int numOfResults;
    private Exception exception;

    public BenchmarkQueryResult(String query){
        this.query = query;
    }

}
