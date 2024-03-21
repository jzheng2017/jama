package nl.jiankai.api;

import java.util.HashSet;
import java.util.Set;

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
}
