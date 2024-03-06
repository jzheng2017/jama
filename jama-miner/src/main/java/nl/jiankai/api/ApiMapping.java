package nl.jiankai.api;

import java.util.Map;

public record ApiMapping(Refactoring.CodeElement original, Refactoring.CodeElement target, RefactoringType refactoringType, String version, Map<String, Object> context) {

}
