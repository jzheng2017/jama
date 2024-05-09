package nl.jiankai.impl.project.maven;


import nl.jiankai.api.project.Dependency;
import nl.jiankai.api.project.Project;
import nl.jiankai.api.project.ProjectData;
import nl.jiankai.api.project.ProjectType;
import nl.jiankai.util.FileUtil;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class MavenProject implements Project {
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
        return FileUtil.findFileRecursive(getLocalPath(), "pom.xml", "");
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
