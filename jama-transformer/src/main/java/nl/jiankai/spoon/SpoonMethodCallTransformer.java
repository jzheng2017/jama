package nl.jiankai.spoon;

import nl.jiankai.api.MethodCallTransformer;
import nl.jiankai.api.Project;
import nl.jiankai.api.ProjectType;
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
    private final Project project;
    private final File targetDirectory;
    private final List<Processor<?>> processors = new ArrayList<>();
    public SpoonMethodCallTransformer(Project project, File targetDirectory) {
        this.project = project;
        this.targetDirectory = targetDirectory;
    }

    @Override
    public void rename(String originalFullyQualifiedName, String newName) {
        processors.add(new AbstractProcessor<CtInvocation<?>>() {
            @Override
            public void process(CtInvocation<?> methodCall) {
                executeIfMethodMatches(methodCall, originalFullyQualifiedName, () -> methodCall.getExecutable().setSimpleName(newName));
            }
        });
    }

    @Override
    public <T> void addArgument(String methodFullyQualifiedName, int position, T value) {
        processors.add(new AbstractProcessor<CtInvocation<?>>() {
            @Override
            public void process(CtInvocation<?> methodCall) {
                executeIfMethodMatches(methodCall, methodFullyQualifiedName, () -> methodCall.addArgumentAt(position, getFactory().Code().createLiteral(value)));
            }
        });
    }


    @Override
    public void removeArgument(String methodFullyQualifiedName, int position) {
        processors.add(new AbstractProcessor<CtInvocation<?>>() {
                    @Override
                    public void process(CtInvocation<?> methodCall) {
                        executeIfMethodMatches(methodCall, methodFullyQualifiedName, () -> methodCall.removeArgument(methodCall.getArguments().get(position)));
                    }
                }
        );
    }

    @Override
    public void swapArguments(String methodFullyQualifiedName, int positionArgument, int positionArgument2) {
        processors.add(new AbstractProcessor<CtInvocation<?>>() {
                    @Override
                    public void process(CtInvocation<?> methodCall) {
                        executeIfMethodMatches(methodCall, methodFullyQualifiedName, () -> Collections.swap(methodCall.getArguments(), positionArgument, positionArgument2));
                    }
                }
        );
    }


    @Override
    public void changeReference(String originalFullyQualifiedName, String newPath) {
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

    private static void executeIfMethodMatches(CtInvocation<?> methodCall, String originalFullyQualifiedName, Runnable action) {
        String path = getFullyQualifiedName(methodCall);

        if (path.equals(originalFullyQualifiedName)) {
            action.run();
        }
    }

    private static String getFullyQualifiedName(CtInvocation<?> methodCall) {
        return getClass(methodCall) + "." + methodCall.getExecutable().getSimpleName() + "(" + String.join(", ", getArgumentTypes(methodCall)) + ")";
    }

    private static List<String> getArgumentTypes(CtInvocation<?> methodCall) {
        return methodCall.getArguments().stream().filter(argument -> argument.getType() != null).map(argument -> argument.getType().getQualifiedName()).toList();
    }

    private static CtTypeReference<?> getClass(CtInvocation<?> methodCall) {
        return methodCall.getExecutable().getDeclaringType();
    }

    private void execute() {

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
