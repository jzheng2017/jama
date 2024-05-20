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

    public static void compile(Project migratedProject, Project originalProject, Project dependencyProject, String newVersion, ElementTransformationTracker tracker) {
        var methodCallTransformationProvider = new SpoonTransformationProvider<CtInvocation>();
        var classTransformationProvider = new SpoonTransformationProvider<CtClass>();
        ProjectCoordinate coord = dependencyProject.getProjectVersion().coordinate();;
        migratedProject.upgradeDependency(new Dependency(coord.groupId(), coord.artifactId(), newVersion));
        migratedProject.install();
        Transformer<Processor<?>> compilationTransformer = new SpoonTransformer(originalProject, migratedProject, migratedProject.getLocalPath());

        compile(migratedProject, compilationTransformer, 1, classTransformationProvider, methodCallTransformationProvider, tracker);
    }

    public static void compile(Project project, Transformer<Processor<?>> transformer, int iterations, TransformationProvider<CtClass> classTransformationProvider, TransformationProvider<CtInvocation> methodCallTransformationProvider, ElementTransformationTracker tracker) {
        if (iterations > MAX_ITERATIONS) {
            return;
        }
        LOGGER.info("Compilation iteration {}", iterations);
        Launcher launcher = getLauncher(project);
        JDTBasedSpoonCompiler modelBuilder = (JDTBasedSpoonCompiler) launcher.getModelBuilder();
        try {
            modelBuilder.build();
            if (!modelBuilder.getProblems().isEmpty()) {
                handleCompilationErrors(project, transformer, iterations, classTransformationProvider, methodCallTransformationProvider, tracker, modelBuilder);
            } else {
                LOGGER.info("Compilation finished with zero errors");
            }
        } catch (ModelBuildingException ignored) {
            handleCompilationErrors(project, transformer, iterations, classTransformationProvider, methodCallTransformationProvider, tracker, modelBuilder);
        }
        tracker.report();
    }

    private static void handleCompilationErrors(Project project, Transformer<Processor<?>> transformer, int iterations, TransformationProvider<CtClass> classTransformationProvider, TransformationProvider<CtInvocation> methodCallTransformationProvider, ElementTransformationTracker tracker, JDTBasedSpoonCompiler modelBuilder) {
        List<CategorizedProblem> problems = modelBuilder.getProblems().stream().filter(CategorizedProblem::isError).toList();
        LOGGER.info("Number of compiler errors: {}", modelBuilder.getProblems().size());
        LOGGER.info("=============================");
        LOGGER.info("Errors (limited to {}):", ERROR_LIMIT);
        problems.stream().limit(ERROR_LIMIT).forEach(problem -> LOGGER.info(problem.toString()));
        LOGGER.info("=============================");
        problems.forEach(problem -> solve(problem, classTransformationProvider, methodCallTransformationProvider, tracker));
        ElementHandler<Processor<?>> classTransformer = new SpoonClassTransformer(classTransformationProvider);
        ElementHandler<Processor<?>> methodCallTransformer = new SpoonMethodCallTransformer(methodCallTransformationProvider);
        transformer.addProcessor(classTransformer.handle());
        transformer.addProcessor(methodCallTransformer.handle());
        transformer.run();
        transformer.reset();
        compile(project, transformer, iterations + 1, classTransformationProvider, methodCallTransformationProvider, tracker);
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
