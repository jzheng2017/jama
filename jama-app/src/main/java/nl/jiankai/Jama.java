package nl.jiankai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.jiankai.api.project.GitRepository;
import nl.jiankai.api.project.ProjectFactory;
import nl.jiankai.impl.project.CompositeProjectFactory;
import nl.jiankai.impl.project.git.GitOperationException;
import nl.jiankai.impl.project.git.JGitRepositoryFactory;
import nl.jiankai.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class Jama {
    private static final Logger LOGGER = LoggerFactory.getLogger(Jama.class);

    //TODO fix sequence issue processors - class/method rename
    //TODO fix generic type is erased and replace with an actual type
    public static void main(String[] args) throws IOException {
        File outputDirectory = new File("/home/jiankai/test");
        GitRepository migratedProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/marklogic-contentpump"));
        GitRepository dependencyProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/commons-collections"));
        Migrator migrator = new Migrator(new File("/home/jiankai/test/" + migratedProject.getLocalPath().getName()));
        migrator.migrate(migratedProject, dependencyProject, "db18992", "1dc530e6", "4.5.0-SNAPSHOT");

//        GitRepository migratedProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/JYso"));
//        GitRepository dependencyProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/jackson-core"));
//        Migrator migrator = new Migrator(new File("/home/jiankai/test/" + migratedProject.getLocalPath().getName()));
//        migrator.migrate(migratedProject, dependencyProject, "03add30", "0c7a329", "4.5.0-SNAPSHOT");

//        GitRepository migratedProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/plugin-test-repo-2"));
//        GitRepository dependencyProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/commons-text"));
//        Migrator migrator = new Migrator(new File("/home/jiankai/test/" + migratedProject.getLocalPath().getName()));
//        migrator.migrate(migratedProject, dependencyProject, "82aecf36", "507a29c1", "1.11.1-SNAPSHOT");

//        runPipeline("/home/jiankai/dev/python/aim_dependents.json", outputDirectory, "db18992", "6b7cf3f6", "4.5.0-SNAPSHOT");
    }

    private static void runPipeline(String repoFile, File outputDirectory, String startCommit, String endCommit, String newVersion) throws IOException {
        JGitRepositoryFactory projectFactory = new JGitRepositoryFactory();
        new ObjectMapper()
                .readValue(new File(repoFile), new TypeReference<List<Repo>>() {
                })
                .parallelStream()
                .map(repo -> {
                    File repoDirectory = createRepoDirectory(outputDirectory, repo);
                    try {
                        return projectFactory.createProject(createGitCloneUrl(repo.url()), repoDirectory);
                    } catch (GitOperationException | IllegalStateException e) {
                        LOGGER.warn("Could not create git repository: {}", e.getMessage());
                        try {
                            FileUtils.deleteDirectory(repoDirectory);
                        } catch (IOException ex) {
                            LOGGER.warn("Could not delete unusable project directory: {}", ex.getMessage());
                        }
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(repo -> migrate(repo, outputDirectory, startCommit, endCommit, newVersion));
    }

    private static void migrate(GitRepository migratedProject, File outputDirectory, String startCommitId, String endCommitId, String newVersion) {
        GitRepository dependencyProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/commons-collections"));
        Migrator migrator = new Migrator(new File(outputDirectory, migratedProject.getLocalPath().getName()));
        migrator.migrate(migratedProject, dependencyProject, startCommitId, endCommitId, newVersion);
    }

    private static String createGitCloneUrl(String url) {
        if (url.endsWith(".git")) {
            return url;
        }

        return url + ".git";
    }

    private static File createRepoDirectory(File outputDirectory, Repo repo) {
        return new File(outputDirectory, repo.url.replace("https://github.com/", "").replace("/", "-"));
    }

    private record Repo(String url, int stars) {
    }
}
