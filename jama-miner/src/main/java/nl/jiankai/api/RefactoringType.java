package nl.jiankai.api;

import java.util.Set;

public enum RefactoringType {
    METHOD_NAME,
    CHANGE_PARAMETER_TYPE,
    CHANGE_RETURN_TYPE,
    ADD_PARAMETER,
    REMOVE_PARAMETER,
    ENCAPSULATE_ATTRIBUTE,
    CLASS_NAME,
    MOVE_CLASS,
    PACKAGE_NAME,
    MOVE_RENAME_CLASS,
    UNKNOWN;

    public boolean isMethodRefactoring() {
        return Set.of
                        (
                                METHOD_NAME,
                                CHANGE_PARAMETER_TYPE,
                                CHANGE_RETURN_TYPE,
                                ADD_PARAMETER,
                                REMOVE_PARAMETER
                        )
                .contains(this);
    }
}
