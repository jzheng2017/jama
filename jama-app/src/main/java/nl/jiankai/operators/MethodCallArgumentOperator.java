package nl.jiankai.operators;

import nl.jiankai.api.MethodCallTransformer;
import nl.jiankai.api.Migration;
import nl.jiankai.api.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MethodCallArgumentOperator implements MigrationOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodCallArgumentOperator.class);
    private final MethodCallTransformer methodCallTransformer;

    public MethodCallArgumentOperator(MethodCallTransformer methodCallTransformer) {
        this.methodCallTransformer = methodCallTransformer;
    }

    @Override
    public void migrate(Migration migration) {
        List<Variable> beforeParameters = (List<Variable>) migration.mapping().context().get("before");
        List<Variable> afterParameters = (List<Variable>) migration.end().context().get("after");

        List<String> before = beforeParameters.stream().map(Variable::name).toList();
        List<String> after = afterParameters.stream().map(Variable::name).toList();

        Set<String> oldSet = new HashSet<>(before);
        Set<String> newSet = new HashSet<>(after);
        List<String> current = new ArrayList<>(before);

        String currentSignature = migration.mapping().original().signature();

        addArguments(newSet, oldSet, after, currentSignature, current);
        removeArguments(oldSet, newSet, current, currentSignature);
        swapArguments(oldSet, newSet, current, after, currentSignature);
    }

    private void addArguments(Set<String> newSet, Set<String> oldSet, List<String> after, String currentSignature, List<String> current) {
        Set<String> added = new HashSet<>(newSet);
        added.removeAll(oldSet);

        for (String param : added) {
            int index = after.indexOf(param);
            methodCallTransformer.addArgument(currentSignature, index);
            current.add(index, param);
        }
    }

    private void removeArguments(Set<String> oldSet, Set<String> newSet, List<String> current, String currentSignature) {
        Set<String> removed = new HashSet<>(oldSet);
        removed.removeAll(newSet);

        for (String param : removed) {
            int index = current.indexOf(param);
            methodCallTransformer.removeArgument(currentSignature, index);
        }
    }

    private void swapArguments(Set<String> oldSet, Set<String> newSet, List<String> current, List<String> after, String currentSignature) {
        List<String> common = new ArrayList<>(oldSet);
        common.retainAll(newSet);
        List<String> swapped = new ArrayList<>();
        for (String param : common) {
            if (current.indexOf(param) != after.indexOf(param)) {
                swapped.add(param);
            }
        }

        for (String param : swapped) {
            int oldIndex = current.indexOf(param);
            int newIndex = after.indexOf(param);
            if (oldIndex != newIndex) {
                Collections.swap(current, oldIndex, newIndex);
                methodCallTransformer.swapArguments(currentSignature, oldIndex, newIndex);
            }
        }
    }
}
