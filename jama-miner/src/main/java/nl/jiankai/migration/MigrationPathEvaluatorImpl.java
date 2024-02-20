package nl.jiankai.migration;

import nl.jiankai.api.ApiMapping;
import nl.jiankai.api.Migration;
import nl.jiankai.api.MigrationPathEvaluator;
import nl.jiankai.api.Refactoring;

import java.util.*;
import java.util.stream.Collectors;

public class MigrationPathEvaluatorImpl implements MigrationPathEvaluator {
    @Override
    public Collection<Migration> evaluate(Collection<Refactoring> refactorings) {
        Map<Integer, List<Refactoring>> methods = refactorings.
                stream()
                .filter(refactoring -> refactoring.refactoringType().isMethodRefactoring())
                .collect(Collectors.groupingBy(Refactoring::sequence));
        List<Migration> migrations = new ArrayList<>();

        for (Integer sequence : methods.keySet()) {
            migrations.addAll(evalBySequence(methods, sequence));
        }
        return migrations;
    }

    private List<Migration> evalBySequence(Map<Integer, List<Refactoring>> refactoredBySequence, int sequence) {
        List<Refactoring> starting = refactoredBySequence.get(sequence);
        List<Migration> migrations = new ArrayList<>();

        for (Refactoring refactoring : starting) {
            List<ApiMapping> mappings = new ArrayList<>();
            ApiMapping lastMapping = toApiMapping(refactoring);
            mappings.add(lastMapping);
            int current = sequence;

            while (current < refactoredBySequence.size()) {
                List<Refactoring> nextRefactorings = refactoredBySequence.get(current + 1);

                for (Refactoring ref : nextRefactorings) {
                    if (ref.before().equals(lastMapping.target())) {
                        lastMapping = toApiMapping(ref);
                        mappings.add(lastMapping);
                        nextRefactorings.remove(ref);
                        break;
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
        return new ApiMapping(refactoring.before(), refactoring.after(), refactoring.refactoringType(), refactoring.commitId());
    }
}
