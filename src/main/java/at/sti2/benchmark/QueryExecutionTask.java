package at.sti2.benchmark;

import at.sti2.engines.BenchmarkEngine;
import at.sti2.exception.MemoryLimitExceededException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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
        try {
            return engine.executeQuery(query);
        } catch (ExecutionException e) {
            log.error("EXECUTION EXCEPTION!");
            throw new ExecutionException(e);
        } catch (OutOfMemoryError e) {
            log.error("Out of memory error in thread!");
            throw new MemoryLimitExceededException(
                "Thread reached the memory limit!");
        }
    }
}
