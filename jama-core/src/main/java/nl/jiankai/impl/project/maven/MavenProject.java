package nl.jiankai.impl.project.maven;


import nl.jiankai.api.project.Dependency;
import nl.jiankai.api.project.Project;
import nl.jiankai.api.project.ProjectData;
import nl.jiankai.api.project.ProjectType;
import nl.jiankai.util.FileUtil;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

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
    public boolean test(Set<String> testClasses) {
        File file = FileUtil.findPomFile(projectRootPath);
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(file);
        if (testClasses.isEmpty()) {
            request.setGoals(Collections.singletonList("test"));
        } else {
            request.setGoals(Collections.singletonList("test -Dtest=%s".formatted(String.join(",", testClasses))));
        }

        Invoker invoker = new DefaultInvoker();

        try {
            LOGGER.info("Running mvn test");
            invoker.execute(request);
            return true;
        } catch (MavenInvocationException e) {
            LOGGER.error("Something went wrong when trying to invoke mvn test", e);
            return false;
        }
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
    public void upgradeDependency(Dependency dependency) {
        dependencyManager.changeDependency(dependency);
    }

    @Override
    public ProjectType getProjectType() {
        return ProjectType.MAVEN;
    }

    @Override
    public Collection<File> getSourceDirectories() {
        return FileUtil
                .findFileRecursive(getLocalPath(), "pom.xml", "")
                .stream()
                .filter(pom -> {
                    File parent = pom.getParentFile();
                    return Objects.requireNonNull(parent.listFiles((FilenameFilter) new NameFileFilter("src"))).length == 1;
                })
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
