package nl.jiankai.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
