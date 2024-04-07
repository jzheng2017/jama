package nl.jiankai;

import nl.jiankai.api.*;
import nl.jiankai.compiler.JDTCompilerProblemSolver;
import nl.jiankai.impl.CompositeProjectFactory;
import nl.jiankai.migration.MethodMigrationPathEvaluatorImpl;
import nl.jiankai.operators.RenameMethodCallOperator;
import nl.jiankai.operators.MethodCallArgumentOperator;
import nl.jiankai.refactoringminer.RefactoringMinerImpl;
import nl.jiankai.spoon.SpoonClassTransformer;
import nl.jiankai.spoon.SpoonMethodCallTransformer;
import nl.jiankai.spoon.SpoonTransformer;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.compiler.ModelBuildingException;
import spoon.processing.Processor;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Migrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Migrator.class);
    private static final int MAX_ITERATIONS = 2;
    private final File outputDirectory;

    public Migrator(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void migrate(GitRepository toMigrateProject, GitRepository dependencyProject, String startCommitId, String endCommitId, String newVersion) {
        try {
            FileUtils.deleteDirectory(outputDirectory);
            FileUtils.copyDirectory(toMigrateProject.getLocalPath(), outputDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        RefactoringMiner refactoringMiner = new RefactoringMinerImpl();
        Collection<Refactoring> refactorings = refactoringMiner.detectRefactoringBetweenCommit(dependencyProject, startCommitId, endCommitId);
        LOGGER.info("Found {} refactorings", refactorings.size());
        Transformer<Processor<?>> transformer = new SpoonTransformer(toMigrateProject, outputDirectory);
        MigrationPathEvaluator migrationPathEvaluator = new MethodMigrationPathEvaluatorImpl();
        migrationPathEvaluator
                .evaluate(refactorings)
                .forEach(migration -> migrate(migration, transformer));
        try {
            transformer.run();
        } catch (ModelBuildingException e) {
            LOGGER.warn("The project has errors", e);
        }
        ProjectCoordinate coord = dependencyProject.getProjectVersion().coordinate();
        Project migratedProject = new CompositeProjectFactory().createProject(outputDirectory);
        Transformer<Processor<?>> compilationTransformer = new SpoonTransformer(migratedProject, outputDirectory);
        migratedProject.upgradeDependency(new Dependency(coord.groupId(), coord.artifactId(), newVersion));
        migratedProject.install();
        compile(migratedProject, compilationTransformer);
    }

    private <T> void compile(Project project, Transformer<T> transformer) {
        compile(project, transformer, 1);
    }

    private <T> void compile(Project project, Transformer<T> transformer, int iterations) {
        if (iterations > MAX_ITERATIONS) {
            return;
        }
        LOGGER.info("Compilation iteration {}", iterations);
        Launcher launcher = new MavenLauncher(project.getLocalPath().getAbsolutePath(), MavenLauncher.SOURCE_TYPE.APP_SOURCE);
        JDTBasedSpoonCompiler modelBuilder = (JDTBasedSpoonCompiler) launcher.getModelBuilder();
        try {
            modelBuilder.build();
            LOGGER.info("Compilation finished with zero errors");
        } catch (ModelBuildingException ignored) {
            LOGGER.info("Number of compiler errors: {}", modelBuilder.getProblems().size());
            LOGGER.info("=============================");
            modelBuilder.getProblems().forEach(problem -> LOGGER.info(problem.toString()));
            LOGGER.info("=============================");
            modelBuilder.getProblems().forEach(problem -> solve(problem, transformer));
            transformer.run();
            transformer.reset();
            compile(project, transformer, iterations + 1);
        }
    }

    private <T> void solve(CategorizedProblem categorizedProblem, Transformer<T> transformer) {
        if (categorizedProblem.getID() == JDTCompilerProblemSolver.MUST_IMPLEMENT_METHOD) {
            List<String> args = Arrays.stream(categorizedProblem.getArguments()).toList();
            transformer.addProcessor((T) new SpoonClassTransformer().implementMethod(args.getLast(), getQualifiedSignature(args)));
        }
    }

    private String getQualifiedSignature(List<String> args) {
        int size = args.size();

        return "%s.%s(%s)".formatted(args.get(size - 2), args.getFirst(), unqualify(args.get(size - 3)));
    }

    private String unqualify(String params) {
        String[] split = params.split(", ");

        return Arrays
                .stream(split)
                .map(param -> {
                    int index = param.lastIndexOf(".") + 1;
                    if (index > 0) {
                        return param.substring(index);
                    } else {
                        return param;
                    }
                })
                .collect(Collectors.joining(", "));
    }

    private void migrate(Migration migration, Transformer<Processor<?>> transformer) {
        MethodCallTransformer<Processor<?>> methodCallTransformer = new SpoonMethodCallTransformer();
        Set<RefactoringType> refactoringTypes = migration.refactorings();
        if (refactoringTypes.contains(RefactoringType.METHOD_NAME)) {
            var rename = new RenameMethodCallOperator<>(methodCallTransformer, transformer);
            rename.migrate(migration);
        }

        if (refactoringTypes.stream().anyMatch(RefactoringType::isMethodParameterRefactoring)) {
            var argumentsOperator = new MethodCallArgumentOperator<>(methodCallTransformer, transformer);
            argumentsOperator.migrate(migration);
        }
    }
}
