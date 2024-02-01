package nl.jiankai.migration;

import nl.jiankai.api.Migration;
import nl.jiankai.api.MigrationPathEvaluator;
import nl.jiankai.api.Refactoring;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MigrationPathEvaluatorImpl implements MigrationPathEvaluator {
    @Override
    public Collection<Migration> evaluate(Collection<Refactoring> refactorings) {
        Map<Integer, List<Refactoring>> methods = refactorings.
                stream()
                .filter(refactoring -> refactoring.refactoringType().isMethodRefactoring())
                .collect(Collectors.groupingBy(Refactoring::sequence));
        return null;
    }
}
