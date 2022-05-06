package at.sti2.model.benchmark_result;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class BenchmarkQueryResult {

    private String query;
    private long timeSpent;
    private int numOfResults;
    private String exception;

    public BenchmarkQueryResult(String query){
        this.query = query;
    }

}