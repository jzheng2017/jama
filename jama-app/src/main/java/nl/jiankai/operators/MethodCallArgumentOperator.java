package nl.jiankai.operators;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.*;
import nl.jiankai.spoon.transformations.method.AddMethodCallArgumentTransformation;
import nl.jiankai.spoon.transformations.method.RemoveMethodCallArgumentTransformation;
import nl.jiankai.spoon.transformations.method.ReplaceMethodCallArgumentTransformation;
import nl.jiankai.spoon.transformations.method.SwapMethodCallArgumentsTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.code.CtInvocation;

import java.util.*;
import java.util.stream.Collectors;

import static nl.jiankai.util.TypeUtil.getDefaultValue;

public class MethodCallArgumentOperator implements MigrationOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodCallArgumentOperator.class);
    private final TransformationProvider<CtInvocation> transformationProvider;
    private final ElementTransformationTracker tracker;

    public MethodCallArgumentOperator(TransformationProvider<CtInvocation> transformationProvider, ElementTransformationTracker tracker) {
        this.transformationProvider = transformationProvider;
        this.tracker = tracker;
    }

    @Override
    public void migrate(Migration migration) {
        List<Variable> beforeParameters = getParameters(migration, "before").getFirst();
        List<Variable> afterParameters = getParameters(migration, "after").getLast();

        Map<String, String> recentNames = getMostRecentNames(migration);
        List<String> beforeWithUpdatedNames = beforeParameters.stream().map(Variable::name).map(name -> getMostRecentName(name, recentNames)).toList();
        List<String> after = afterParameters.stream().map(Variable::name).toList();

        Set<String> oldSet = new HashSet<>(beforeWithUpdatedNames);
        Set<String> newSet = new HashSet<>(after);
        List<String> current = new ArrayList<>(beforeWithUpdatedNames);

        String currentSignature = migration.mapping().original().signature();

        addArguments(newSet, oldSet, afterParameters, currentSignature, current);
        removeArguments(oldSet, newSet, current, currentSignature);
        swapArguments(oldSet, newSet, current, after, currentSignature);

        Map<String, String> changedVariables = getChangedTypeVariables(migration);
        updateArgumentTypes(afterParameters, changedVariables, currentSignature);
    }

    private void updateArgumentTypes(List<Variable> after, Map<String, String> changedVariables, String originalSignature) {
        List<String> afterNames = after.stream().map(Variable::name).toList();
        changedVariables = changedVariables.entrySet().stream().filter(entry -> !isBoxed(entry.getKey(), entry.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        for (Map.Entry<String, String> variable : changedVariables.entrySet()) {
            int index = afterNames.indexOf(variable.getValue());
            if (index >= 0) {
                transformationProvider.produce(originalSignature, new ReplaceMethodCallArgumentTransformation<>(tracker, originalSignature, index, getDefaultValue(after.get(index).type())));
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

    private <T> void addArguments(Set<String> newSet, Set<String> oldSet, List<Variable> afterParameters, String currentSignature, List<String> current) {
        Set<String> added = new HashSet<>(newSet);
        added.removeAll(oldSet);
        List<String> after = afterParameters.stream().map(Variable::name).toList();


        for (String param : added) {
            int index = after.indexOf(param);
            if (index >= current.size()) { // possible if param added during same commit
                current.add(param);
                transformationProvider.produce(currentSignature, new AddMethodCallArgumentTransformation<>(tracker, getDefaultValue(afterParameters.get(index).type()), currentSignature, -1));
            } else {
                current.add(index, param);
                transformationProvider.produce(currentSignature, new AddMethodCallArgumentTransformation<>(tracker, getDefaultValue(afterParameters.get(index).type()), currentSignature, index));
            }
        }
    }


    private void removeArguments(Set<String> oldSet, Set<String> newSet, List<String> current, String currentSignature) {
        Set<String> removed = new HashSet<>(oldSet);
        removed.removeAll(newSet);

        for (String param : removed) {
            int index = current.indexOf(param);
            transformationProvider.produce(currentSignature, new RemoveMethodCallArgumentTransformation(tracker, currentSignature, index));
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
                transformationProvider.produce(currentSignature, new SwapMethodCallArgumentsTransformation(tracker, currentSignature, oldIndex, newIndex));
            }
        }
    }

    private List<List<Variable>> getParameters(Migration migration, String key) {
        Migration current = migration;
        List<List<Variable>> parameters = new ArrayList<>();
        while (current != null) {
            if (current.mapping().refactoringType().isMethodParameterRefactoring()) {
                List<Map<String, String>> params = (List<Map<String, String>>) current.mapping().context().get(key);
                List<Variable> variables = params.stream().map(m -> new Variable(m.get("type"), m.get("name"))).toList();
                parameters.add(variables);
            }
            current = current.next();
        }

        return parameters;
    }

    private Map<String, String> getChangedTypeVariables(Migration migration) {
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
                String original = (String) current.mapping().context().get("original");
                String renamed = (String) current.mapping().context().get("renamed");

                if (names.containsKey(renamed)) {
                    names.remove(renamed);
                } else {
                    names.put(original, renamed);
                }

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
