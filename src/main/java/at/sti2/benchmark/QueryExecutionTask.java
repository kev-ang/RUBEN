package at.sti2.benchmark;

import at.sti2.engines.BenchmarkEngine;
import at.sti2.exception.MemoryLimitExceededException;
import java.util.concurrent.Callable;

public class QueryExecutionTask implements Callable<Integer> {

    private final BenchmarkEngine engine;
    private final String query;

    public QueryExecutionTask(BenchmarkEngine engine, String query) {
        this.engine = engine;
        this.query = query;
    }

    @Override
    public Integer call() throws Exception {
        try {
            return engine.executeQuery(query);
        } catch (OutOfMemoryError e) {
            throw new MemoryLimitExceededException("Thread reached the memory limit!");
        }
    }
}
