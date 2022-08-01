package at.sti2.benchmark;

import at.sti2.engines.RuleEngine;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;

/**
 * Task used to evaluate a single query on a given rule engine.
 *
 * @author kevin.angele@sti2.at
 */
@Slf4j
public class QueryExecutionTask implements Callable<Integer> {

    private final RuleEngine engine;
    private final String query;

    public QueryExecutionTask(RuleEngine engine, String query) {
        this.engine = engine;
        this.query = query;
    }

    @Override
    public Integer call() throws Exception {
        return engine.executeQuery(query);
    }
}