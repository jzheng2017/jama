package nl.jiankai.api;

public record ApiMapping(Refactoring.CodeElement original, Refactoring.CodeElement target, RefactoringType refactoringType, String version) {

}
