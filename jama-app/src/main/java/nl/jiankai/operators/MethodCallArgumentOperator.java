package nl.jiankai.operators;

import nl.jiankai.api.MethodCallTransformer;
import nl.jiankai.api.Migration;
import nl.jiankai.api.RefactoringType;
import nl.jiankai.api.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class MethodCallArgumentOperator implements MigrationOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodCallArgumentOperator.class);
    private final MethodCallTransformer methodCallTransformer;

    public MethodCallArgumentOperator(MethodCallTransformer methodCallTransformer) {
        this.methodCallTransformer = methodCallTransformer;
    }

    @Override
    public void migrate(Migration migration) {
        List<Variable> beforeParameters = getParameters(migration, "before").getFirst();
        List<Variable> afterParameters = getParameters(migration, "after").getLast();

        List<String> before = beforeParameters.stream().map(Variable::name).toList();
        List<String> after = afterParameters.stream().map(Variable::name).toList();

        Set<String> oldSet = new HashSet<>(before);
        Set<String> newSet = new HashSet<>(after);
        List<String> current = new ArrayList<>(before);

        String currentSignature = migration.mapping().original().signature();

        addArguments(newSet, oldSet, afterParameters, currentSignature, current);
        removeArguments(oldSet, newSet, current, currentSignature);
        swapArguments(oldSet, newSet, current, after, currentSignature);

        Map<String, String> changedVariables = getChangedVariables(migration);
        updateArgumentTypes(afterParameters, changedVariables, currentSignature);
    }

    private void updateArgumentTypes(List<Variable> after, Map<String, String> changedVariables, String currentSignature) {
        List<String> afterNames = after.stream().map(Variable::name).toList();
        changedVariables = changedVariables.entrySet().stream().filter(entry -> !isBoxed(entry.getKey(), entry.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        for (Map.Entry<String, String> variable : changedVariables.entrySet()) {
            int index = afterNames.indexOf(variable.getValue());
            if (index >= 0) {
                methodCallTransformer.replaceArgument(currentSignature, index, getDefaultValue(after.get(index).type()));
            }
        }
    }

    private boolean isBoxed(String originalType, String newType) {
        return Set.of("int", "Integer").containsAll(List.of(originalType, newType)) ||
                Set.of("byte", "Byte").containsAll(List.of(originalType, newType)) ||
                Set.of("boolean", "Boolean").containsAll(List.of(originalType, newType)) ||
                Set.of("long", "Long").containsAll(List.of(originalType, newType)) ||
                Set.of("float", "Float").containsAll(List.of(originalType, newType)) ||
                Set.of("double", "Double").containsAll(List.of(originalType, newType)) ||
                Set.of("char", "Character").containsAll(List.of(originalType, newType));
    }

    private void addArguments(Set<String> newSet, Set<String> oldSet, List<Variable> afterParameters, String currentSignature, List<String> current) {
        Set<String> added = new HashSet<>(newSet);
        added.removeAll(oldSet);
        List<String> after = afterParameters.stream().map(Variable::name).toList();


        for (String param : added) {
            int index = after.indexOf(param);
            methodCallTransformer.addArgument(currentSignature, index, getDefaultValue(afterParameters.get(index).type()));
            current.add(index, param);
        }
    }

    private <T> T getDefaultValue(String type) {
        return (T) switch (type) {
            case "int", "Integer", "short", "Short", "byte", "Byte" -> 0;
            case "long", "Long" -> 0L;
            case "char" -> '\u0000';
            case "String" -> "";
            case "float", "Float" -> 0.0f;
            case "double", "Double" -> 0.0d;
            case "BigDecimal" -> BigDecimal.ZERO;
            case "BigInteger" -> BigInteger.ZERO;
            case "boolean", "Boolean" -> false;
            default -> null;
        };
    }

    private void removeArguments(Set<String> oldSet, Set<String> newSet, List<String> current, String currentSignature) {
        Set<String> removed = new HashSet<>(oldSet);
        removed.removeAll(newSet);

        for (String param : removed) {
            int index = current.indexOf(param);
            methodCallTransformer.removeArgument(currentSignature, index);
            current.remove(index);
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

    private List<List<Variable>> getParameters(Migration migration, String key) {
        Migration current = migration;
        List<List<Variable>> parameters = new ArrayList<>();
        while (current != null) {
            if (current.mapping().refactoringType().isMethodParameterRefactoring()) {
                parameters.add((List<Variable>) current.mapping().context().get(key));
            }
            current = current.next();
        }

        return parameters;
    }

    private Map<String, String> getChangedVariables(Migration migration) {
        Migration current = migration;
        Set<String> variables = new HashSet<>();
        Map<String, String> names = getMostRecentNames(migration);

        while (current != null) {
            if (current.mapping().refactoringType() == RefactoringType.CHANGE_PARAMETER_TYPE) {
                variables.add((String) current.mapping().context().get("changedTypeVariable"));
            }

            current = current.next();
        }


        return variables.stream().collect(Collectors.toMap(v -> v, v -> getMostRecentName(v, names)));
    }

    private Map<String, String> getMostRecentNames(Migration migration) {
        Migration current = migration;
        Map<String, String> names = new HashMap<>();

        while (current != null) {
            if (current.mapping().refactoringType() == RefactoringType.RENAME_PARAMETER) {
                names.put((String) current.mapping().context().get("original"), (String) current.mapping().context().get("renamed"));
            }

            current = current.next();
        }

        return names;
    }

    private String getMostRecentName(String name, Map<String, String> mapping) {
        String mostRecentName = mapping.getOrDefault(name, name);

        while (mapping.containsKey(mostRecentName)) {
            mostRecentName = mapping.get(mostRecentName);
        }

        return mostRecentName;
    }
}
