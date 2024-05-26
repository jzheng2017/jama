package nl.jiankai.compiler;

public record CompilationResult(int iteration, long errors, long warnings, long info) {
}
