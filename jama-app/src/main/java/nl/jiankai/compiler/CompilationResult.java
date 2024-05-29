package nl.jiankai.compiler;

import java.util.List;

public record CompilationResult(int iteration, long errors, long warnings, long info, List<CompilerError> compilerErrors) {
    public record CompilerError(int id, String message) {
    }
}
