package nl.jiankai.impl.project.git;

public class GitOperationException extends RuntimeException {

    public GitOperationException(String message) {
        super(message);
    }
    public GitOperationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
