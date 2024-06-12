package nl.jiankai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.jiankai.api.*;
import nl.jiankai.api.project.Dependency;
import nl.jiankai.api.project.GitRepository;
import nl.jiankai.api.project.Project;
import nl.jiankai.api.project.ProjectData;
import nl.jiankai.api.storage.CacheService;
import nl.jiankai.impl.JacksonSerializationService;
import nl.jiankai.impl.project.git.GitOperationException;
import nl.jiankai.impl.project.git.JGitRepositoryFactory;
import nl.jiankai.impl.storage.MultiFileCacheService;
import nl.jiankai.migration.MethodMigrationPathEvaluatorImpl;
import nl.jiankai.refactoringminer.RefactoringMinerImpl;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static nl.jiankai.spoon.SpoonUtil.getLauncher;

public class Jama {
    private static final Logger LOGGER = LoggerFactory.getLogger(Jama.class);
    public static final Path BASE_PATH = Paths.get("").toAbsolutePath();

    //TODO fix generic type is erased and replace with an actual type
    public static void main(String[] args) throws IOException {
        pipeline();
    }

    private static void pipeline() throws IOException {
        File outputDirectory = BASE_PATH.toFile();
        String[] inputParams = FileUtils.readFileToString(new File("input.txt")).split(";");
        String dependencyProjectUrl = inputParams[0];
        String startCommitId = inputParams[1];
        String endCommitId = inputParams[2];
        String oldVersion = "4.0";
        String newVersion = "4.5.0-SNAPSHOT";
        GitRepository dependencyProject = new JGitRepositoryFactory().createProject(dependencyProjectUrl, new File(outputDirectory, "dependency"));
        dependencyProject.checkout(endCommitId);
        Collection<Refactoring> refactorings = getRefactorings(dependencyProject, startCommitId, endCommitId);
        Collection<Migration> migrations = getMigrationPaths(refactorings);
        LOGGER.info("Found {} migration paths", migrations.size());
        Launcher dependencyLauncher = getLauncher(dependencyProject);
        dependencyLauncher.buildModel();
        LOGGER.info("{} build sucessfully", dependencyProject.getId());
        runPipeline("deps.json", outputDirectory, dependencyProject, migrations, dependencyLauncher, oldVersion, newVersion);
    }

    private static void commonsCollection() {
        File outputDirectory = BASE_PATH.toFile();
        String startCommitId = "db18992";
        String endCommitId = "6b7cf3f6";
        GitRepository dependencyProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/commons-collections"));
        dependencyProject.checkout(endCommitId);
        Collection<Refactoring> refactorings = getRefactorings(dependencyProject, startCommitId, endCommitId);
        Collection<Migration> migrations = getMigrationPaths(refactorings);
        LOGGER.info("Found {} migration paths", migrations.size());
        Launcher dependencyLauncher = getLauncher(dependencyProject);
        dependencyLauncher.buildModel();
        GitRepository migratedProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/ysoserialinject"));
        migrate(migratedProject, dependencyProject, outputDirectory, migrations, dependencyLauncher, "4.5.0-SNAPSHOT"); //collections
    }

    private static void commonsText() {
        File outputDirectory = BASE_PATH.toFile();
        String startCommitId = "82aecf36";
        String endCommitId = "bcd37271";
        GitRepository dependencyProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/commons-text"));
        dependencyProject.checkout(endCommitId);
        Collection<Refactoring> refactorings = getRefactorings(dependencyProject, startCommitId, endCommitId);
        Collection<Migration> migrations = getMigrationPaths(refactorings);
        LOGGER.info("Found {} migration paths", migrations.size());
        Launcher dependencyLauncher = getLauncher(dependencyProject);
        dependencyLauncher.buildModel();
        LOGGER.info("{} build sucessfully", dependencyProject.getId());
        GitRepository migratedProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/plugin-test-repo-2"));
        migrate(migratedProject, dependencyProject, outputDirectory, migrations, dependencyLauncher, "1.11.1-SNAPSHOT"); //commons-text
    }

    private static Collection<Migration> getMigrationPaths(Collection<Refactoring> refactorings) {
        var migrationPathEvaluator = new MethodMigrationPathEvaluatorImpl();
        return migrationPathEvaluator.evaluate(refactorings);
    }

    private static Collection<Refactoring> getRefactorings(GitRepository dependencyProject, String startCommitId, String endCommitId) {
        RefactoringMiner refactoringMiner = new RefactoringMinerImpl();
        Collection<Refactoring> refactorings;
        CacheService<RefactoringCollection> cacheService = new MultiFileCacheService<>(BASE_PATH + "/cache", new JacksonSerializationService(), RefactoringCollection.class);

        String identifier = startCommitId + ":" + endCommitId;

        if (cacheService.isCached(identifier)) {
            refactorings = cacheService
                    .get(identifier)
                    .orElseGet(() -> new RefactoringCollection(startCommitId, endCommitId, refactoringMiner.detectRefactoringBetweenCommit(dependencyProject, startCommitId, endCommitId)))
                    .refactorings();
        } else {
            refactorings = refactoringMiner.detectRefactoringBetweenCommit(dependencyProject, startCommitId, endCommitId);
            RefactoringCollection refactoringCollection = new RefactoringCollection(startCommitId, endCommitId, refactorings);
            cacheService.write(refactoringCollection);
        }

        LOGGER.info("Found {} refactorings", refactorings.size());
        return refactorings;
    }

    private static void runPipeline(String repoFile, File outputDirectory, GitRepository dependencyProject, Collection<Migration> migrations, Launcher dependencyLauncher, String oldVersion, String newVersion) throws IOException {
        try {
            JGitRepositoryFactory projectFactory = new JGitRepositoryFactory();
            new ObjectMapper()
                    .readValue(new File(repoFile), new TypeReference<List<Repo>>() {
                    })
                    .parallelStream()
                    .filter(repo -> repo.stars > 5)
                    .map(repo -> {
                        File repoDirectory = createRepoDirectory(outputDirectory, repo);
                        try {
                            return projectFactory.createProject(createGitCloneUrl(repo.url()), repoDirectory);
                        } catch (GitOperationException | IllegalStateException e) {
                            LOGGER.warn("Could not create git repository: {}", e.getMessage());
                            deleteRepo(repoDirectory);
                            return null;
                        } catch (Exception e) {
                            LOGGER.warn("Unknown error occurred while creating git repository: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(repo -> {
                        if (isEligible(repo, dependencyProject, oldVersion)) {
                            LOGGER.info("Repository '{}' is eligible", repo.getId());
                            return true;
                        } else {
                            deleteRepo(repo.getLocalPath());
                            return false;
                        }
                    })
                    .forEach(repo -> {
                        try {
                            migrate(repo, dependencyProject, outputDirectory, migrations, dependencyLauncher, newVersion);
                        } catch (Exception e) {
                            LOGGER.error("Something went wrong while migrating: {}", e.getMessage());
                        }
                    });
        } finally {
            FileUtils.deleteDirectory(new File(BASE_PATH.toFile(), "migrated"));
        }
    }

    private static boolean isEligible(Project repo, Project dependencyProject, String oldVersion) {
        Collection<File> sourceDirectories = repo.getSourceDirectories();

        if (sourceDirectories.size() != 1) {
            return false;
        }

        File sourceDirectory = sourceDirectories.iterator().next();
        final boolean srcDirectoryInRoot = sourceDirectory.getParentFile().equals(repo.getLocalPath());

        return repo.isOlderDependency(createDependency(dependencyProject.getProjectVersion(), oldVersion)) && srcDirectoryInRoot;
    }

    private static void deleteRepo(File repoDirectory) {
        try {
            FileUtils.deleteDirectory(repoDirectory);
        } catch (IOException ex) {
            LOGGER.warn("Could not delete unusable project directory: {}", ex.getMessage());
        }
    }

    private static Dependency createDependency(ProjectData projectVersion, String newVersion) {
        return new Dependency(projectVersion.coordinate().groupId(), projectVersion.coordinate().artifactId(), newVersion);
    }

    private static void migrate(Project migratedProject, Project dependencyProject, File outputDirectory, Collection<Migration> migrations, Launcher dependencyLauncher, String newVersion) {
        Migrator migrator = new Migrator(new File(outputDirectory, Paths.get("migrated", migratedProject.getLocalPath().getName()).toString()));
        Migrator.Statistics statistics = migrator.migrate(migratedProject, dependencyProject, migrations, dependencyLauncher, newVersion);
        CacheService<Migrator.Statistics> cacheService = new MultiFileCacheService<>(new File(BASE_PATH.toFile(), Paths.get("results", LocalDate.now().toString()).toString()).toString(), new JacksonSerializationService(), Migrator.Statistics.class);
        cacheService.write(statistics);
    }

    private static String createGitCloneUrl(String url) {
        if (url.endsWith(".git")) {
            return url;
        }

        return url + ".git";
    }

    private static File createRepoDirectory(File outputDirectory, Repo repo) {
        return new File(outputDirectory, Paths.get("original", repo.url.replace("https://github.com/", "").replace("/", "-")).toString());
    }

    private record Repo(String url, int stars) {
    }

    @JsonIgnoreProperties({"id"})
    private record RefactoringCollection(String startCommit, String endCommit,
                                         Collection<Refactoring> refactorings) implements Identifiable {
        @Override
        public String getId() {
            return startCommit + ":" + endCommit;
        }
    }
}
