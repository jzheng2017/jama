package nl.jiankai.api;

import java.util.Collection;

public interface MigrationPathEvaluator {
    Collection<Migration> evaluate(Collection<Refactoring> refactorings);
}
