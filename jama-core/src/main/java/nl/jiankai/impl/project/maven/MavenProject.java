package nl.jiankai.impl.project.maven;


import nl.jiankai.api.project.*;
import nl.jiankai.util.FileUtil;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenProject implements Project {
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenProject.class);
    private final File projectRootPath;
    private final MavenProjectDependencyResolver dependencyResolver = new MavenProjectDependencyResolver();
    private final MavenDependencyManager dependencyManager;

    public MavenProject(File projectRootPath) {
        this.projectRootPath = projectRootPath;
        this.dependencyManager = new MavenDependencyManager(FileUtil.findPomFile(projectRootPath));
    }

    @Override
    public File getLocalPath() {
        return projectRootPath;
    }

    @Override
    public Collection<Dependency> resolve() {
        return dependencyResolver.resolve(getLocalPath());
    }

    @Override
    public Collection<File> jars() {
        return dependencyResolver.jars(getLocalPath());
    }

    @Override
    public void install() {
        dependencyResolver.install(getLocalPath());
    }

    @Override
    public TestReport test(Set<String> testClasses) {
        File file = FileUtil.findPomFile(projectRootPath);
        OutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        PrintStreamHandler printStreamHandler = new PrintStreamHandler(printStream, true);
        InvocationRequest request = new DefaultInvocationRequest();
        request.setOutputHandler(printStreamHandler);
        request.setErrorHandler(printStreamHandler);
        request.setPomFile(file);

        if (testClasses.isEmpty()) {
            request.setGoals(Collections.singletonList("test"));
        } else {
            request.setGoals(Collections.singletonList("test -Dtest=%s".formatted(String.join(",", testClasses))));
        }
        Invoker invoker = new DefaultInvoker();

        try {
            LOGGER.info("Running mvn test");
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() == 0) {
                return getTestReport(outputStream.toString());
            } else {
                LOGGER.warn("Failed to run mvn test: {}", outputStream);
                return TestReport.failure(outputStream.toString());
            }
        } catch (MavenInvocationException e) {
            LOGGER.error("Something went wrong when trying to invoke mvn test", e);
            return TestReport.failure(e.getMessage());
        } finally {
            LOGGER.info(outputStream.toString());
        }
    }

    private TestReport getTestReport(String output) {
        Pattern pattern = Pattern.compile("Tests run:\\s*(\\d+),\\s*Failures:\\s*(\\d+),\\s*Errors:\\s*(\\d+),\\s*Skipped:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(output);
        TestReport testReport = TestReport.failure(output);

        while (matcher.find()) {
            int totalTests = Integer.parseInt(matcher.group(1));
            int failures = Integer.parseInt(matcher.group(2));
            int errors = Integer.parseInt(matcher.group(3));
            int skipped = Integer.parseInt(matcher.group(4));
            int successful = totalTests - failures - errors - skipped;
            testReport = new TestReport(totalTests, successful, errors, failures, skipped, true, "");
        }

        return testReport;
    }

    @Override
    public ProjectData getProjectVersion() {
        return dependencyResolver.getProjectVersion(getLocalPath());
    }

    @Override
    public boolean hasDependency(Dependency dependency) {
        return resolve().contains(dependency);
    }

    @Override
    public boolean isOlderDependency(Dependency dependency) {
        return resolve()
                .stream()
                .anyMatch(actualDependency -> actualDependency.groupId().equals(dependency.groupId()) &&
                        actualDependency.artifactId().equals(dependency.artifactId()) &&
                        (new ComparableVersion(actualDependency.version()).compareTo(new ComparableVersion(dependency.version())) >= 0));
    }

    @Override
    public void upgradeDependency(Dependency dependency) {
        dependencyManager.changeDependency(dependency);
    }

    @Override
    public ProjectType getProjectType() {
        return ProjectType.MAVEN;
    }

    @Override
    public Collection<File> getSourceDirectories() {
        File sourceDirectory = new File(getLocalPath(), "src");

        if (sourceDirectory.exists() && sourceDirectory.isDirectory()) {
            return Collections.singletonList(sourceDirectory);
        }

        return FileUtil
                .findFileRecursive(getLocalPath(), "pom.xml", "")
                .stream()
                .filter(pom -> {
                    File parent = pom.getParentFile();
                    return Objects.requireNonNull(parent.listFiles((FilenameFilter) new NameFileFilter("src"))).length == 1;
                })
                .map(file -> new File(file.getParentFile(), "src"))
                .toList();
    }

    @Override
    public String getId() {
        return getLocalPath().getAbsolutePath();
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenProject that = (MavenProject) o;
        return Objects.equals(projectRootPath, that.projectRootPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectRootPath);
    }
}
