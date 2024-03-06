package nl.jiankai.spoon;

import nl.jiankai.api.MethodCallTransformer;
import nl.jiankai.api.Project;
import nl.jiankai.api.ProjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpoonMethodCallTransformer implements MethodCallTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpoonMethodCallTransformer.class);
    private final Project project;
    private final File targetDirectory;
    private final List<Processor<?>> processors = new ArrayList<>();
    public SpoonMethodCallTransformer(Project project, File targetDirectory) {
        this.project = project;
        this.targetDirectory = targetDirectory;
    }

    @Override
    public void rename(String originalSignature, String newName) {
        LOGGER.info("Renaming method name of all methods with signature '{}' to the name '{}'", originalSignature, newName);
        processors.add(new AbstractProcessor<CtInvocation<?>>() {
            @Override
            public void process(CtInvocation<?> methodCall) {
                executeIfMethodMatches(methodCall, originalSignature, () -> methodCall.getExecutable().setSimpleName(newName));
            }
        });
    }

    @Override
    public <T> void addArgument(String methodSignature, int position, T value) {
        LOGGER.info("Adding a new argument to the method '{}' at position {}", methodSignature, position);

        processors.add(new AbstractProcessor<CtInvocation<?>>() {
            @Override
            public void process(CtInvocation<?> methodCall) {
                executeIfMethodMatches(methodCall, methodSignature, () -> methodCall.addArgumentAt(position, getFactory().Code().createLiteral(value)));
            }
        });
    }


    @Override
    public void removeArgument(String methodSignature, int position) {
        LOGGER.info("Removing an argument from the method '{}' at position {}", methodSignature, position);

        processors.add(new AbstractProcessor<CtInvocation<?>>() {
                    @Override
                    public void process(CtInvocation<?> methodCall) {
                        executeIfMethodMatches(methodCall, methodSignature, () -> methodCall.removeArgument(methodCall.getArguments().get(position)));
                    }
                }
        );
    }

    @Override
    public void swapArguments(String methodSignature, int positionArgument, int positionArgument2) {
        LOGGER.info("Swapping arguments of method calls to '{}' at position {} and {}", methodSignature, positionArgument, positionArgument2);

        processors.add(new AbstractProcessor<CtInvocation<?>>() {
                    @Override
                    public void process(CtInvocation<?> methodCall) {
                        executeIfMethodMatches(methodCall, methodSignature, () -> Collections.swap(methodCall.getArguments(), positionArgument, positionArgument2));
                    }
                }
        );
    }


    @Override
    public void changeReference(String methodSignature, String newPath) {
//        execute(new AbstractProcessor<CtInvocation<?>>() {
//            @Override
//            public void process(CtInvocation<?> methodCall) {
//                executeIfMethodMatches(methodCall, originalFullyQualifiedName, () -> methodCall
//                        .getExecutable()
//                        .setDeclaringType(
//                                methodCall
//                                        .getFactory()
//                                        .Type()
//                                        .get(newPath)
//                                        .getReference()
//                        )
//                );
//            }
//        });
        // TODO
    }

    private static void executeIfMethodMatches(CtInvocation<?> methodCall, String originalSignature, Runnable action) {
        String path = getSignature(methodCall);

        if (path.equals(originalSignature)) {
            action.run();
        }
    }

    private static String getSignature(CtInvocation<?> methodCall) {
        return getClass(methodCall) + "." + methodCall.getExecutable().getSimpleName() + "(" + String.join(", ", getArgumentTypes(methodCall)) + ")";
    }

    private static List<String> getArgumentTypes(CtInvocation<?> methodCall) {
        return methodCall.getArguments().stream().filter(argument -> argument.getType() != null).map(argument -> argument.getType().getSimpleName()).toList();
    }

    private static CtTypeReference<?> getClass(CtInvocation<?> methodCall) {
        return methodCall.getExecutable().getDeclaringType();
    }

    private Launcher getLauncher() {
        return switch (project.getProjectType()) {
            case ProjectType.MAVEN -> new MavenLauncher(project.getLocalPath().getAbsolutePath(), MavenLauncher.SOURCE_TYPE.ALL_SOURCE);
            case UNKNOWN -> throw new UnsupportedOperationException("Unsupported project type");
        };
    }

    @Override
    public void run() {
        Launcher launcher = getLauncher();
        processors.forEach(launcher::addProcessor);
        launcher.setSourceOutputDirectory(targetDirectory);
        launcher.getEnvironment().setPrettyPrinterCreator(() -> {
            DefaultJavaPrettyPrinter printer = new DefaultJavaPrettyPrinter(launcher.getEnvironment());
            printer.setIgnoreImplicit(false);
            return printer;
        });
        launcher.run();
    }
}
