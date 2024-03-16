package nl.jiankai.api;

import java.util.Map;
import java.util.Objects;

public record Refactoring(String commitId, int sequence, CodeElement before, CodeElement after, RefactoringType refactoringType, Map<String, Object> context) {
    public record CodeElement(String name, String path, String signature, Position position, String filePath){
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CodeElement that = (CodeElement) o;
            return Objects.equals(signature, that.signature);
        }

        @Override
        public int hashCode() {
            return Objects.hash(signature);
        }
    }
}
