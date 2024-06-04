package nl.jiankai.api.project;

public record TestReport(int total, int successful, int errors, int failures, int skipped,
                         boolean testSuiteRanSuccessfully, String failureReason) {

    public static TestReport failure(String failureReason) {
        String ansiRegex = "\\u001B\\[[;\\d]*m";
        return new TestReport(0, 0, 0, 0, 0, false, failureReason.replaceAll(ansiRegex, ""));
    }
}
