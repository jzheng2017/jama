package nl.jiankai.api;

import java.util.Set;

public enum RefactoringType {
    METHOD_NAME,
    CHANGE_PARAMETER_TYPE,
    CHANGE_RETURN_TYPE,
    ADD_PARAMETER,
    REMOVE_PARAMETER,
    RENAME_PARAMETER,
    ENCAPSULATE_ATTRIBUTE,
    CLASS_NAME,
    MOVE_CLASS,
    REMOVE_CLASS,
    PACKAGE_NAME,
    MOVE_RENAME_CLASS,
    REORDER_PARAMETER,
    UNKNOWN;

    public boolean isMethodRefactoring() {
        return Set.of
                        (
                                METHOD_NAME,
                                CHANGE_PARAMETER_TYPE,
                                CHANGE_RETURN_TYPE,
                                ADD_PARAMETER,
                                REMOVE_PARAMETER,
                                REORDER_PARAMETER,
                                RENAME_PARAMETER
                        )
                .contains(this);
    }

    public boolean isMethodParameterRefactoring() {
        return Set.of
                        (
                                CHANGE_PARAMETER_TYPE,
                                ADD_PARAMETER,
                                REMOVE_PARAMETER,
                                REORDER_PARAMETER,
                                RENAME_PARAMETER
                        )
                .contains(this);
    }

    public boolean isAttributeRefactoring() {
        return Set.of(
                ENCAPSULATE_ATTRIBUTE
        ).contains(this);
    }
}
