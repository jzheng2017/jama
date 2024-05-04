package nl.jiankai.api;

import java.util.*;

public record Migration(ApiMapping mapping, Migration next) {
    public ApiMapping end() {
        Migration current = this;

        while (current.next != null) {
            current = current.next;
        }

        return current.mapping;
    }

    public Set<RefactoringType> refactorings() {
        Migration current = this;
        Set<RefactoringType> refactoringTypes = new HashSet<>();

        while (current != null) {
            refactoringTypes.add(current.mapping.refactoringType());
            current = current.next;
        }

        return refactoringTypes;
    }

    public boolean hasRefactorings(Set<RefactoringType> refactorings) {
        return refactorings().containsAll(refactorings);
    }

    public boolean hasAnyRefactoring(Set<RefactoringType> refactorings) {
        return refactorings().stream().anyMatch(refactorings::contains);
    }

    public boolean hasRefactoring(RefactoringType refactoringType) {
        return refactorings().contains(refactoringType);
    }

    /**
     * Returns the last intermediate migration of mapping A to mapping B of a specific refactoring type
     * @param refactoringType the refactoring type to find
     * @return the last found migration
     */
    public Optional<Migration> lastOf(RefactoringType refactoringType) {
        return lastOf(Set.of(refactoringType));
    }

    public Optional<Migration> lastOf(Set<RefactoringType> refactoringTypes) {
        Migration current = this;
        Optional<Migration> lastKnown = Optional.empty();

        while (current != null) {
            if (refactoringTypes.contains(current.mapping.refactoringType())) {
                lastKnown = Optional.of(current);
            }
            current = current.next;
        }

        return lastKnown;
    }

    public List<Object> getContext(String key) {
        Migration current = this;
        List<Object> context = new ArrayList<>();

        while (current != null) {
            if (current.mapping.context().containsKey(key)) {
                context.add(current.mapping.context().get(key));
            }
            current = current.next;
        }

        return context;
    }
}
