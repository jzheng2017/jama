package nl.jiankai.api.project;

import nl.jiankai.api.Identifiable;

import java.io.File;
import java.util.Collection;
import java.util.Set;

public interface Project extends Identifiable {
    /**
     * @return the local file path of the git repository
     */
    File getLocalPath();

    /**
     * Resolves all the dependencies of a project
     * @return all dependencies
     */
    Collection<Dependency> resolve();

    /**
     * Resolves the file locations of all the jars of the dependencies that the project depends on
     * @return list of file locations of the jars
     */
    Collection<File> jars();

    /**
     * Download all dependencies of the project to a local repository
     */
    void install();

    TestReport test(Set<String> testClasses);

    /**
     * @return the project details containing group id, artifact id and the version
     */
    ProjectData getProjectVersion();

    /**
     * @param dependency the dependency to check
     * @return whether the projects depend on it
     */
    boolean hasDependency(Dependency dependency);

    boolean isOlderDependency(Dependency dependency);

    /**
     * Upgrade a dependency
     * @param dependency the dependency
     */
    void upgradeDependency(Dependency dependency);

    ProjectType getProjectType();

    Collection<File> getSourceDirectories();
}
