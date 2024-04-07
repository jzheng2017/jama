package nl.jiankai.compiler;

import nl.jiankai.Migrator;
import nl.jiankai.api.Project;
import nl.jiankai.api.Transformer;
import nl.jiankai.spoon.SpoonClassTransformer;
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

public class JDTCompilerProblemSolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(JDTCompilerProblemSolver.class);
    private static final int MAX_ITERATIONS = 2;

    public static final int METHOD_UNDEFINED = 67108964;
    public static final int MUST_IMPLEMENT_METHOD = 67109264;
    public static final int METHOD_ARGUMENTS_PROVIDED_NOT_APPLICABLE = 67108979;

    public static <T> void compile(Project project, Transformer<T> transformer) {
        compile(project, transformer, 1);
    }

    public static <T> void compile(Project project, Transformer<T> transformer, int iterations) {
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

    private static  <T> void solve(CategorizedProblem categorizedProblem, Transformer<T> transformer) {
        if (categorizedProblem.getID() == MUST_IMPLEMENT_METHOD) {
            List<String> args = Arrays.stream(categorizedProblem.getArguments()).toList();
            transformer.addProcessor((T) new SpoonClassTransformer().implementMethod(args.getLast(), getQualifiedSignature(args)));
        }
    }

    private static String getQualifiedSignature(List<String> args) {
        int size = args.size();

        return "%s.%s(%s)".formatted(args.get(size - 2), args.getFirst(), unqualify(args.get(size - 3)));
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
