package nl.jiankai.api;

public record Refactoring(String commitId, int sequence, CodeElement before, CodeElement after, RefactoringType refactoringType) {
    public record CodeElement(String name, String path, Position position, String filePath){
        public String fullPath() {
            return path + "." + name;
        }
    }
}
