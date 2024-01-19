package nl.jiankai.api;

import java.util.Collection;

public interface MigrationPathEvaluator {
    Collection<Migration> evaluate(GitRepository repository, Collection<Refactoring> refactorings);
}
