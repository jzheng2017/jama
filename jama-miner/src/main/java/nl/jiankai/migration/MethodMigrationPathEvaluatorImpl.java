package nl.jiankai.migration;

import nl.jiankai.api.ApiMapping;
import nl.jiankai.api.Migration;
import nl.jiankai.api.MigrationPathEvaluator;
import nl.jiankai.api.Refactoring;

import java.util.*;
import java.util.stream.Collectors;

public class MethodMigrationPathEvaluatorImpl implements MigrationPathEvaluator {
    @Override
    public Collection<Migration> evaluate(Collection<Refactoring> refactorings) {
        Map<Integer, List<Refactoring>> methods = refactorings.
                stream()
                .filter(refactoring -> refactoring.refactoringType().isMethodRefactoring())
                .collect(Collectors.groupingBy(Refactoring::sequence));
        List<Migration> migrations = new ArrayList<>();
        int maxSeq = methods.keySet().stream().max(Integer::compareTo).orElse(1);
        for (Integer sequence : methods.keySet()) {
            migrations.addAll(evalBySequence(methods, sequence, maxSeq));
        }
        return migrations;
    }

    private List<Migration> evalBySequence(Map<Integer, List<Refactoring>> refactoredBySequence, int sequence, int maxSeq) {
        List<Refactoring> starting = refactoredBySequence.get(sequence);
        List<Migration> migrations = new ArrayList<>();

        Map<Refactoring.CodeElement, List<Refactoring>> refs = starting.stream().collect(Collectors.groupingBy(Refactoring::before));
        for (List<Refactoring> refactorings : refs.values()) {
            List<ApiMapping> mappings = refactorings.stream().map(this::toApiMapping).collect(Collectors.toList());
            ApiMapping lastMapping = mappings.getLast();
            int current = sequence;

            while (current <= maxSeq) {
                List<Refactoring> nextRefactorings = refactoredBySequence.get(current + 1);
                if (nextRefactorings == null) {
                    current++;
                    continue;
                }

                ApiMapping start = lastMapping;

                int index = 0;
                while (index < nextRefactorings.size()) {
                    Refactoring ref = nextRefactorings.get(index);
                    if (ref.before().equals(start.target())) {
                        lastMapping = toApiMapping(ref);
                        mappings.add(lastMapping);
                        nextRefactorings.remove(ref);
                    } else {
                        index++;
                    }
                }

                current++;
            }


            migrations.add(toMigration(mappings, 0));
        }

        return migrations;
    }

    private Migration toMigration(List<ApiMapping> apiMappings, int currentIndex) {
        if (currentIndex >= apiMappings.size()) {
            return null;
        }

        return new Migration(apiMappings.get(currentIndex), toMigration(apiMappings, currentIndex + 1));
    }

    private ApiMapping toApiMapping(Refactoring refactoring) {
        return new ApiMapping(refactoring.before(), refactoring.after(), refactoring.refactoringType(), refactoring.commitId(), refactoring.context());
    }
}
