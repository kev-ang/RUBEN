package at.sti2.benchmark;

import at.sti2.engines.BenchmarkEngine;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryExecutionTask implements Callable<Integer> {

    private final BenchmarkEngine engine;
    private final String query;

    public QueryExecutionTask(BenchmarkEngine engine, String query) {
        this.engine = engine;
        this.query = query;
    }

    @Override
    public Integer call() throws Exception {
        return engine.executeQuery(query);
    }
}