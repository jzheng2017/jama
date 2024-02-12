package nl.jiankai.migration;

import nl.jiankai.api.ApiMapping;
import nl.jiankai.api.Migration;
import nl.jiankai.api.MigrationPathEvaluator;
import nl.jiankai.api.Refactoring;

import java.util.ArrayList;
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
        List<Migration> migrations = new ArrayList<>();
//        for (Map.Entry<Integer, List<Refactoring>> entry: methods.entrySet()) {
//            int seq = entry.getKey();
//            List<Refactoring> refs = entry.getValue();
//            int next = seq+1;
//            Migration migration;
//            for (Refactoring refactoring : refs) {
//                migration = new Migration(toApiMapping(refactoring), wq)
//            }
//            migrations.add(migration);
//        }
        return migrations;
    }

    private ApiMapping toApiMapping(Refactoring refactoring) {
        return new ApiMapping(refactoring.before(), refactoring.after(), refactoring.refactoringType(), refactoring.commitId());
    }
}
