package nl.jiankai.api.project;

public record TestReport(int total, int successful, int errors, int failures, int skipped, boolean testSuiteRanSuccessfully) {

    public static TestReport failure() {
        return new TestReport(0,0,0,0,0,false);
    }
}
