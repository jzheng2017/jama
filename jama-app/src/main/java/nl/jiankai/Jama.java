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
import nl.jiankai.impl.storage.LocalFileStorageService;
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
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        String[] inputParams = FileUtils.readFileToString(new File(BASE_PATH.toFile(), "input.txt")).split(";");
        String dependencyProjectUrl = inputParams[0];
        String startCommitId = inputParams[1];
        String endCommitId = inputParams[2];
        String oldVersion = inputParams[3];
        String newVersion = inputParams[4];
        LOGGER.info("Starting pipeline with the following parameters: dependencyUrl: {}, startCommitId {}, endCommitId {}, oldVersion: {}, newVersion: {}", dependencyProjectUrl, startCommitId, endCommitId, oldVersion, newVersion);
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

    private static void jodatime() {
        File outputDirectory = BASE_PATH.toFile();
        String startCommitId = "c5a5190";
        String endCommitId = "290a451";
        GitRepository dependencyProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/joda-time"));
        dependencyProject.checkout(endCommitId);
        Collection<Refactoring> refactorings = getRefactorings(dependencyProject, startCommitId, endCommitId);
        Collection<Migration> migrations = getMigrationPaths(refactorings);
        LOGGER.info("Found {} migration paths", migrations.size());
        Launcher dependencyLauncher = getLauncher(dependencyProject);
        dependencyLauncher.buildModel();
        GitRepository migratedProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/jenkins-sync-plugin"));
        LOGGER.info("Migrating jodatime");
        migrate(migratedProject, dependencyProject, outputDirectory, migrations, dependencyLauncher, "2.12.7");
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
        GitRepository migratedProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/opencsv-source"));
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
        JGitRepositoryFactory projectFactory = new JGitRepositoryFactory();
        LocalFileStorageService analysedDependents = new LocalFileStorageService(BASE_PATH + "/analyzed.txt", true);
        LocalFileStorageService eligibleDependents = new LocalFileStorageService(BASE_PATH + "/eligible.txt", true);
        Set<String> alreadyAnalysedDependents = analysedDependents.read().collect(Collectors.toSet());

        new ObjectMapper()
                .readValue(new File(BASE_PATH.toFile(), repoFile), new TypeReference<List<Repo>>() {
                })
                .parallelStream()
                .filter(repo -> repo.stars > 5)
                .filter(shouldSkip(alreadyAnalysedDependents))
                .map(createRepository(outputDirectory, analysedDependents, projectFactory))
                .filter(Objects::nonNull)
                .filter(isEligibleForAnalysis(dependencyProject, oldVersion))
                .forEach(migrate(outputDirectory, dependencyProject, migrations, dependencyLauncher, newVersion, eligibleDependents));
    }

    private static Consumer<GitRepository> migrate(File outputDirectory, GitRepository dependencyProject, Collection<Migration> migrations, Launcher dependencyLauncher, String newVersion, LocalFileStorageService eligibleDependents) {
        return repo -> {
            try {
                eligibleDependents.append(repo.getId());
                migrate(repo, dependencyProject, outputDirectory, migrations, dependencyLauncher, newVersion);
            } catch (Exception e) {
                LOGGER.error("Something went wrong while migrating: {}", e.getMessage());
            }
        };
    }

    private static Predicate<GitRepository> isEligibleForAnalysis(GitRepository dependencyProject, String oldVersion) {
        return repo -> {
            if (isEligible(repo, dependencyProject, oldVersion)) {
                LOGGER.info("Repository '{}' is eligible", repo.getId());
                return true;
            } else {
                deleteRepo(repo.getLocalPath());
                LOGGER.info("Repository '{}' is not eligible", repo.getId());
                return false;
            }
        };
    }

    private static Function<Repo, GitRepository> createRepository(File outputDirectory, LocalFileStorageService analysedDependents, JGitRepositoryFactory projectFactory) {
        return repo -> {
            analysedDependents.append(repo.url);
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
        };
    }

    private static Predicate<Repo> shouldSkip(Set<String> alreadyAnalysedDependents) {
        return repo -> {
            boolean shouldSkip = alreadyAnalysedDependents.contains(repo.url);

            if (shouldSkip) {
                LOGGER.info("Skipped {}", repo.url);
                return false;
            }

            return true;
        };
    }

    private static boolean isEligible(Project repo, Project dependencyProject, String oldVersion) {
        try {
            Collection<File> sourceDirectories = repo.getSourceDirectories();

            if (sourceDirectories.size() != 1) {
                LOGGER.info("Project '{}' has multiple source directories", repo.getId());
                return false;
            }

            File sourceDirectory = sourceDirectories.iterator().next();


            final boolean srcDirectoryInRoot = sourceDirectory.getParentFile().equals(repo.getLocalPath());

            if (!srcDirectoryInRoot) {
                LOGGER.error("Project '{}' source directory is not in the root folder", repo.getId());
                return false;
            }

            try {
                return repo.isOlderDependency(createDependency(dependencyProject.getProjectVersion(), oldVersion));
            } catch (Exception e) {
                LOGGER.error("Something went wrong while checking dependency version for project '{}': {}", repo.getId(), e.getMessage());
                return true; //if dependency version can not be checked then assume it's okay because resolving maven pom file is unstable
            }
        } catch (Exception e) {
            LOGGER.error("Something went wrong while determining eligibility of project {}: {}", repo.getId(), e.getMessage());
            return false;
        }
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
        try {
            Migrator migrator = new Migrator(new File(outputDirectory, Paths.get("migrated", migratedProject.getLocalPath().getName()).toString()));
            Migrator.Statistics statistics = migrator.migrate(migratedProject, dependencyProject, migrations, dependencyLauncher, newVersion);
            CacheService<Migrator.Statistics> cacheService = new MultiFileCacheService<>(new File(BASE_PATH.toFile(), Paths.get("results", LocalDate.now().toString()).toString()).toString(), new JacksonSerializationService(), Migrator.Statistics.class);
            cacheService.write(statistics);
            LOGGER.info("Successfully wrote project '{}' migration results", migratedProject.getLocalPath());
        } catch (Exception e) {
            LOGGER.error("Something went wrong while migrating and results could not be written for project '{}': {}", migratedProject.getLocalPath(), e.getMessage(), e);
        }
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
