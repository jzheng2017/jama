package nl.jiankai.operators;

import nl.jiankai.api.MethodCallTransformer;
import nl.jiankai.api.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ReorderArgumentMethodCallOperator implements MigrationOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReorderArgumentMethodCallOperator.class);
    private final MethodCallTransformer methodCallTransformer;

    public ReorderArgumentMethodCallOperator(MethodCallTransformer methodCallTransformer) {
        this.methodCallTransformer = methodCallTransformer;
    }

    @Override
    public void migrate(Migration migration) {
        List<String> before = (List<String>) migration.mapping().context().get("before");
        List<String> after = (List<String>) migration.mapping().context().get("after");

        if (before.size() != after.size()) {
            LOGGER.warn("Unequal list of parameter sizes, before {}, after {}", before, after);
            return;
        }

        //TODO find reorder params

//        methodCallTransformer.swapArguments(migration.mapping().original().signature(), changedPositions.get(0), changedPositions.get(1));
    }
}
