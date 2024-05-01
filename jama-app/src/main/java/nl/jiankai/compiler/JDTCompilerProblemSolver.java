package nl.jiankai.compiler;

import nl.jiankai.Migrator;
import nl.jiankai.api.ElementTransformationTracker;
import nl.jiankai.api.Project;
import nl.jiankai.api.Transformer;
import nl.jiankai.spoon.SpoonClassTransformer;
import nl.jiankai.spoon.SpoonStatementCleaner;
import nl.jiankai.spoon.SpoonStatementTransformer;
import nl.jiankai.spoon.SpoonUtil;
import nl.jiankai.util.FileUtil;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.compiler.ModelBuildingException;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nl.jiankai.spoon.SpoonUtil.getLauncher;

public class JDTCompilerProblemSolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(JDTCompilerProblemSolver.class);
    private static final int MAX_ITERATIONS = 2;
    private static final int ERROR_LIMIT = 50;
    private static final ElementTransformationTracker tracker = new ElementTransformationTracker();
    public static final int METHOD_UNDEFINED = 67108964;
    public static final int MUST_OVERRIDE_OR_IMPLEMENT_SUPERTYPE_METHOD = 67109498;
    public static final int MUST_IMPLEMENT_METHOD = 67109264;
    public static final int METHOD_ARGUMENTS_PROVIDED_NOT_APPLICABLE = 67108979;
    public static final int CANNOT_BE_RESOLVED_TO_A_TYPE = 16777218;
    public static final int UNHANDLED_EXCEPTION = 16777384;

    public static <T> void compile(Project project, Transformer<T> transformer) {
        compile(project, transformer, 1);
    }

    public static <T> void compile(Project project, Transformer<T> transformer, int iterations) {
        if (iterations > MAX_ITERATIONS) {
            return;
        }
        LOGGER.info("Compilation iteration {}", iterations);
        Launcher launcher = getLauncher(project);
        JDTBasedSpoonCompiler modelBuilder = (JDTBasedSpoonCompiler) launcher.getModelBuilder();
        try {
            modelBuilder.build();
            LOGGER.info("Compilation finished with zero errors");
        } catch (ModelBuildingException ignored) {
            List<CategorizedProblem> problems = modelBuilder.getProblems().stream().filter(CategorizedProblem::isError).toList();
            LOGGER.info("Number of compiler errors: {}", modelBuilder.getProblems().size());
            LOGGER.info("=============================");
            LOGGER.info("Errors (limited to {}):", ERROR_LIMIT);
            problems.stream().limit(ERROR_LIMIT).forEach(problem -> LOGGER.info(problem.toString()));
            LOGGER.info("=============================");
            problems.forEach(problem -> solve(problem, transformer));
            transformer.run();
            transformer.reset();
            compile(project, transformer, iterations + 1);
        }
        tracker.report();
    }

    private static <T> void solve(CategorizedProblem categorizedProblem, Transformer<T> transformer) {
        List<String> args = Arrays.stream(categorizedProblem.getArguments()).toList();
        int size = args.size();
        if (categorizedProblem.getID() == MUST_IMPLEMENT_METHOD) {
            String qualifiedSignature = "%s#%s(%s)".formatted(args.get(size - 2), args.getFirst(), unqualify(args.get(size - 3)));
            transformer.addProcessor((T) new SpoonClassTransformer(tracker).implementMethod(args.getLast(), qualifiedSignature));
        } else if (categorizedProblem.getID() == MUST_OVERRIDE_OR_IMPLEMENT_SUPERTYPE_METHOD) {
            String qualifiedSignature = "%s#%s(%s)".formatted(args.getLast(), args.getFirst(), unqualify(args.get(1)));
            transformer.addProcessor((T) new SpoonClassTransformer(tracker).removeMethod(args.getLast(), qualifiedSignature));
        } else if (categorizedProblem.getID() == METHOD_UNDEFINED) {
            String qualifiedSignature = "%s(%s)".formatted(args.get(1), unqualify(args.getLast()));
            transformer.addProcessor((T) new SpoonStatementCleaner(tracker).removeMethodCall(qualifiedSignature));
        } else if (categorizedProblem.getID() == CANNOT_BE_RESOLVED_TO_A_TYPE) {
            String className = FileUtil.javaFileNameToFullyQualifiedClass(new String(categorizedProblem.getOriginatingFileName()));
            transformer.addProcessor((T) new SpoonClassTransformer(tracker).removeParent(className, args.getFirst()));
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
