package nl.jiankai.spoon;

import nl.jiankai.api.Method;
import nl.jiankai.api.MethodCallTransformer;
import spoon.Launcher;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
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
    public void rename(Method original, Method target) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(new FileSystemFolder(sourceDirectory));

        launcher.addProcessor(new AbstractProcessor<CtInvocation<?>>() {
            @Override
            public void process(CtInvocation<?> method) {
                String path = getPath(method);

                if (path.equals(original.path())) {
                    method.getExecutable().setSimpleName(target.name());
                }
            }

            private static String getPath(CtInvocation<?> methodCall) {
                return methodCall.getExecutable().getDeclaringType().toString() + "." + methodCall.getExecutable().getSimpleName();
            }
        });

        launcher.setSourceOutputDirectory(targetDirectory);
        launcher.run();
    }
}
