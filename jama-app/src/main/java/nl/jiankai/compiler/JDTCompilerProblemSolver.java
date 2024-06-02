package nl.jiankai.compiler;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.*;
import nl.jiankai.api.project.Dependency;
import nl.jiankai.api.project.Project;
import nl.jiankai.api.project.ProjectCoordinate;
import nl.jiankai.impl.project.CompositeProjectFactory;
import nl.jiankai.spoon.SpoonClassTransformer;
import nl.jiankai.spoon.SpoonMethodCallTransformer;
import nl.jiankai.spoon.SpoonTransformer;
import nl.jiankai.spoon.transformations.SpoonTransformationProvider;
import nl.jiankai.spoon.transformations.clazz.ImplementMethodTransformation;
import nl.jiankai.spoon.transformations.clazz.RemoveMethodTransformation;
import nl.jiankai.spoon.transformations.clazz.RemoveParentClassTransformation;
import nl.jiankai.spoon.transformations.method.RemoveMethodCallTransformation;
import nl.jiankai.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.compiler.ModelBuildingException;
import spoon.processing.Processor;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static nl.jiankai.spoon.SpoonUtil.getLauncher;

public class JDTCompilerProblemSolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(JDTCompilerProblemSolver.class);
    private static final int MAX_ITERATIONS = 2;
    private static final int ERROR_LIMIT = 50;
    public static final int METHOD_UNDEFINED = 67108964;
    public static final int MUST_OVERRIDE_OR_IMPLEMENT_SUPERTYPE_METHOD = 67109498;
    public static final int MUST_IMPLEMENT_METHOD = 67109264;
    public static final int METHOD_ARGUMENTS_PROVIDED_NOT_APPLICABLE = 67108979;
    public static final int CANNOT_BE_RESOLVED_TO_A_TYPE = 16777218;
    public static final int UNHANDLED_EXCEPTION = 16777384;

    public static List<CompilationResult> compile(Project migratedProject, Project originalProject, Project dependencyProject, String newVersion, ElementTransformationTracker tracker) {
        ProjectCoordinate coord = dependencyProject.getProjectVersion().coordinate();

        migratedProject.upgradeDependency(new Dependency(coord.groupId(), coord.artifactId(), newVersion));
        migratedProject.install();

        List<CompilationResult> result = compile(originalProject, migratedProject, 1, tracker, new ArrayList<>());
        tracker.report();
        return result;
    }

    public static List<CompilationResult> compile(Project originalProject, Project migratedProject, int iterations,
                                               ElementTransformationTracker tracker,
                                                  List<CompilationResult> results) {
        if (iterations > MAX_ITERATIONS) {
            return results;
        }


        LOGGER.info("Compilation iteration {}", iterations);
        Launcher launcher = getLauncher(migratedProject);
        JDTBasedSpoonCompiler modelBuilder = (JDTBasedSpoonCompiler) launcher.getModelBuilder();
        try {
            modelBuilder.build();
            if (!modelBuilder.getProblems().isEmpty()) {
                handleCompilationErrors(originalProject, migratedProject, iterations, tracker, modelBuilder, results);
            } else {
                LOGGER.info("Compilation finished with zero errors");
                results.add(new CompilationResult(
                        iterations,
                        0,
                        modelBuilder.getProblems().stream().filter(CategorizedProblem::isWarning).count(),
                        modelBuilder.getProblems().stream().filter(CategorizedProblem::isInfo).count(),
                        getCompilerErrors(modelBuilder.getProblems())
                ));
            }
        } catch (ModelBuildingException ignored) {
            handleCompilationErrors(originalProject, migratedProject, iterations, tracker, modelBuilder, results);
        }

        return results;
    }

    private static List<CompilationResult.CompilerError> getCompilerErrors(List<CategorizedProblem> problems) {
        return problems.stream().map(problem -> new CompilationResult.CompilerError(problem.getID(), problem.getMessage())).collect(Collectors.toList());
    }

    private static void handleCompilationErrors(Project originalProject, Project migratedProject, int iterations,
                                                ElementTransformationTracker tracker, JDTBasedSpoonCompiler modelBuilder,
                                                List<CompilationResult> results) {
        results.add(new CompilationResult(iterations,
                modelBuilder.getProblems().stream().filter(CategorizedProblem::isError).count(),
                modelBuilder.getProblems().stream().filter(CategorizedProblem::isWarning).count(),
                modelBuilder.getProblems().stream().filter(CategorizedProblem::isInfo).count(),
                getCompilerErrors(modelBuilder.getProblems())));
        List<CategorizedProblem> problems = modelBuilder.getProblems().stream().filter(CategorizedProblem::isError).toList();
        LOGGER.info("Number of compiler errors: {}", problems.size());
        LOGGER.info("=============================");
        LOGGER.info("Errors (limited to {}):", ERROR_LIMIT);
        problems.stream().limit(ERROR_LIMIT).forEach(problem -> LOGGER.info(problem.toString()));
        LOGGER.info("=============================");
        var methodCallTransformationProvider = new SpoonTransformationProvider<CtInvocation>();
        var classTransformationProvider = new SpoonTransformationProvider<CtClass>();
        File destination = new File(migratedProject.getLocalPath() + "_tmp");
        Transformer<Processor<?>> transformer = new SpoonTransformer(originalProject, migratedProject, destination);

        try {
            FileUtils.copyDirectory(migratedProject.getLocalPath(), destination);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        problems.forEach(problem -> solve(problem, classTransformationProvider, methodCallTransformationProvider, tracker));
        ElementHandler<Processor<?>> classTransformer = new SpoonClassTransformer(classTransformationProvider);
        ElementHandler<Processor<?>> methodCallTransformer = new SpoonMethodCallTransformer(methodCallTransformationProvider, tracker);
        transformer.addProcessor(classTransformer.handle());
        transformer.addProcessor(methodCallTransformer.handle());
        transformer.run();
        transformer.reset();

        try {
            FileUtils.deleteDirectory(migratedProject.getLocalPath());
            FileUtils.moveDirectory(destination, migratedProject.getLocalPath());
            FileUtils.deleteDirectory(destination);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        compile(originalProject, migratedProject, iterations + 1, tracker, results);
    }

    private static void solve(CategorizedProblem categorizedProblem, TransformationProvider<CtClass> classTransformationProvider, TransformationProvider<CtInvocation> methodCallTransformationProvider, ElementTransformationTracker tracker) {
        List<String> args = Arrays.stream(categorizedProblem.getArguments()).toList();
        int size = args.size();
        if (categorizedProblem.getID() == MUST_IMPLEMENT_METHOD) {
            String qualifiedSignature = "%s#%s(%s)".formatted(args.get(size - 2), args.getFirst(), unqualify(args.get(size - 3)));
            classTransformationProvider.add(args.getLast(), new ImplementMethodTransformation(qualifiedSignature, tracker));
        } else if (categorizedProblem.getID() == MUST_OVERRIDE_OR_IMPLEMENT_SUPERTYPE_METHOD) {
            String qualifiedSignature = "%s#%s(%s)".formatted(args.getLast(), args.getFirst(), unqualify(args.get(1)));
            classTransformationProvider.add(args.getLast(), new RemoveMethodTransformation(qualifiedSignature, tracker));
        } else if (categorizedProblem.getID() == METHOD_UNDEFINED) {
            String qualifiedSignature = "%s(%s)".formatted(args.get(1), unqualify(args.getLast()));
            methodCallTransformationProvider.add(qualifiedSignature, new RemoveMethodCallTransformation(qualifiedSignature, tracker));
        } else if (categorizedProblem.getID() == CANNOT_BE_RESOLVED_TO_A_TYPE) {
            String className = FileUtil.javaFileNameToFullyQualifiedClass(new String(categorizedProblem.getOriginatingFileName()));
            classTransformationProvider.add(className, new RemoveParentClassTransformation(className, args.getLast(), tracker));
        }
    }

    private static String unqualify(String params) {
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
}
