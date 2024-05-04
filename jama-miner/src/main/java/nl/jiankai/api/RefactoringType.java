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
    ADD_THROWN_EXCEPTION,
    MOVE_METHOD,
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
                                RENAME_PARAMETER,
                                MOVE_METHOD
                        )
                .contains(this);
    }

    public boolean isMethodReferenceRefactoring() {
        return Set.of(
                MOVE_CLASS,
                METHOD_NAME,
                MOVE_METHOD
        ).contains(this);
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

    public static Set<RefactoringType> methodParameterRefactoring() {
        return Set.of
                (
                        CHANGE_PARAMETER_TYPE,
                        ADD_PARAMETER,
                        REMOVE_PARAMETER,
                        REORDER_PARAMETER,
                        RENAME_PARAMETER
                );
    }

    public boolean isBreaking() {
        return Set.of(METHOD_NAME,
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
                ADD_THROWN_EXCEPTION,
                MOVE_METHOD).contains(this);
    }

    public boolean isAttributeRefactoring() {
        return Set.of(
                ENCAPSULATE_ATTRIBUTE
        ).contains(this);
    }

    public boolean isExceptionRefactoring() {
        return Set.of(
                ADD_THROWN_EXCEPTION
        ).contains(this);
    }
}
