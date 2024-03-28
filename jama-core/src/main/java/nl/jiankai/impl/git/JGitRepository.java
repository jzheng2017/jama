package nl.jiankai.impl.git;

import nl.jiankai.api.*;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class JGitRepository implements GitRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(JGitRepository.class);
    private final Git git;
    private final Project project;

    public JGitRepository(Git git, Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Plain projects are not supported currently");
        }
        this.git = git;
        this.project = project;
    }

    @Override
    public String getId() {
        return project.getId();
    }

    @Override
    public File getLocalPath() {
        String path = git.getRepository().getDirectory().getAbsolutePath();
        if (path.endsWith("/.git")) {
            return new File(path.substring(0, path.length() - "/.git".length()));
        } else {
            return new File(path);
        }
    }

    @Override
    public Collection<Dependency> resolve() {
        return project.resolve();
    }

    @Override
    public Collection<File> jars() {
        return project.jars();
    }

    @Override
    public void install() {
        project.install();
    }

    @Override
    public ProjectData getProjectVersion() {
        return project.getProjectVersion();
    }

    @Override
    public boolean hasDependency(Dependency dependency) {
        return project.hasDependency(dependency);
    }

    @Override
    public void upgradeDependency(Dependency dependency) {
        project.upgradeDependency(dependency);
    }

    @Override
    public ProjectType getProjectType() {
        return project.getProjectType();
    }

    @Override
    public String toString() {
        return project.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JGitRepository that = (JGitRepository) o;
        return Objects.equals(project, that.project);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project);
    }

    public Git getGit() {
        return git;
    }

    @Override
    public void checkout(String commitId, List<String> paths) {
        try {
            CheckoutCommand checkoutCommand = git.checkout();
            checkoutCommand.setStartPoint(commitId);
            checkoutCommand.setName(commitId);
            if (!paths.isEmpty()) {
                checkoutCommand.addPaths(paths);
            }
            checkoutCommand.call();
            LOGGER.info("Checked out commit '{}' for project '{}'", commitId, getId());
        } catch (GitAPIException e) {
            LOGGER.warn("Could not checkout commit '{}'", commitId, e);
            throw new GitOperationException("Could not checkout commit '%s'".formatted(commitId));
        }
    }
}
