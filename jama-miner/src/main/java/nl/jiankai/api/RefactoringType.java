package nl.jiankai.api;

import java.util.Set;

public enum RefactoringType {
    METHOD_NAME,
    METHOD_SIGNATURE,
    UNKNOWN;

    public boolean isMethodRefactoring() {
        return Set.of(METHOD_NAME, METHOD_SIGNATURE).contains(this);
    }
}
