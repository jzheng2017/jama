package nl.jiankai.spoon;

import nl.jiankai.api.Method;
import nl.jiankai.api.MethodCallTransformer;
import spoon.Launcher;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.compiler.FileSystemFolder;

import java.io.File;

public class SpoonMethodCallTransformer implements MethodCallTransformer {
    private final File sourceDirectory;
    private final File targetDirectory;

    public SpoonMethodCallTransformer(File sourceDirectory, File targetDirectory) {
        this.sourceDirectory = sourceDirectory;
        this.targetDirectory = targetDirectory;
    }

    @Override
    public void rename(Method original, String newName) {
        execute(new AbstractProcessor<CtInvocation<?>>() {
            @Override
            public void process(CtInvocation<?> methodCall) {
                executeIfMethodMatches(methodCall, original.path(), () -> methodCall.getExecutable().setSimpleName(newName));
            }
        });
    }

    @Override
    public void changeReference(Method original, String newPath) {
        execute(new AbstractProcessor<CtInvocation<?>>() {
            @Override
            public void process(CtInvocation<?> methodCall) {
                executeIfMethodMatches(methodCall, original.path(), () -> {
                // TODO
                });
            }
        });
    }

    private static void executeIfMethodMatches(CtInvocation<?> methodCall, String originalPath, Runnable action) {
        String path = getPath(methodCall);

        if (path.equals(originalPath)) {
            action.run();
        }
    }

    private static String getPath(CtInvocation<?> methodCall) {
        return getClass(methodCall) + "." + methodCall.getExecutable().getSimpleName();
    }

    private static CtTypeReference<?> getClass(CtInvocation<?> methodCall) {
        return methodCall.getExecutable().getDeclaringType();
    }

    private void execute(AbstractProcessor<? extends CtElement> processor) {
        Launcher launcher = new Launcher();

        launcher.addInputResource(new FileSystemFolder(sourceDirectory));
        launcher.addProcessor(processor);
        launcher.setSourceOutputDirectory(targetDirectory);

        launcher.run();
    }
}
