package nl.jiankai.api;

import java.util.Map;

public record Refactoring(String commitId, int sequence, CodeElement before, CodeElement after, RefactoringType refactoringType, Map<String, Object> context) {
    public record CodeElement(String name, String path, String signature, Position position, String filePath){ }
}
