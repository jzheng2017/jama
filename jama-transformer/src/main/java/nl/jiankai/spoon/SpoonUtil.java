package nl.jiankai.spoon;

import nl.jiankai.api.Project;
import nl.jiankai.api.ProjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.support.sniper.SniperJavaPrettyPrinter;

import java.util.List;

public class SpoonUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpoonUtil.class);

    public static Launcher getLauncher(Project project) {
        Launcher launcher = switch (project.getProjectType()) {
            case ProjectType.MAVEN ->
                    new MavenLauncher(project.getLocalPath().getAbsolutePath(), MavenLauncher.SOURCE_TYPE.APP_SOURCE);
            case UNKNOWN -> throw new UnsupportedOperationException("Unsupported project type");
        };

        launcher.getEnvironment().setAutoImports(true);
        launcher.getEnvironment()
                .setPrettyPrinterCreator(() -> {
                    DefaultJavaPrettyPrinter printer = new SniperJavaPrettyPrinter(launcher.getEnvironment());
                    printer.setIgnoreImplicit(false);
                    return printer;
                });
        launcher.getEnvironment().setNoClasspath(true); //which allows us to suppress compilation errors (no exception thrown)

        return launcher;
    }

    public static String getSignature(CtMethod<?> method) {
        return getClass(method) + "#" + method.getReference().getSimpleName() + "(" + String.join(", ", getArgumentTypes(method)) + ")";
    }

    public static String getSignature(CtInvocation<?> methodCall) {
        return getClass(methodCall) + "#" + methodCall.getExecutable().getSimpleName() + "(" + String.join(", ", getArgumentTypes(methodCall)) + ")";
    }

    public static String getSignatureWithoutClass(CtInvocation<?> methodCall) {
        return methodCall.getExecutable().getSimpleName() + "(" + String.join(", ", getArgumentTypes(methodCall)) + ")";
    }

    public static void executeIfMethodCallMatches(CtInvocation<?> methodCall, String originalSignature, Runnable action) {
        String path = getSignature(methodCall);

        if (path.equals(originalSignature)) {
            action.run();
        }
    }

    public static String inferMethodCallType(CtInvocation<?> methodCall) {
        if (methodCall.getType() != null) {
            return methodCall.getType().getSimpleName();
        }

        CtElement parent = methodCall.getParent();

        if (parent instanceof CtInvocation<?> parentMethodCall) {
            int index = parentMethodCall.getArguments().indexOf(methodCall);
            LOGGER.info("Inferring method call type for '{}'", methodCall.getExecutable().getSignature());
            return parentMethodCall.getExecutable().getParameters().get(index).getSimpleName();
        }

        return "";
    }

    private static List<String> getArgumentTypes(CtMethod<?> method) {
        return method.getParameters().stream().map(c -> c.getReference().getType().getSimpleName()).toList();
    }

    private static List<String> getArgumentTypes(CtInvocation<?> methodCall) {
        return methodCall.getExecutable().getParameters().stream().map(CtTypeReference::getSimpleName).toList();
    }

    private static CtTypeReference<?> getClass(CtMethod<?> method) {
        return method.getDeclaringType().getReference();
    }

    private static CtTypeReference<?> getClass(CtInvocation<?> methodCall) {
        return methodCall.getExecutable().getDeclaringType();
    }

    public static String getSignature(CtClass<?> ctClass) {
        return ctClass.getReference().getQualifiedName();
    }

    public static String getSignature(CtFieldAccess fieldAccess) {
        return fieldAccess.getVariable().getQualifiedName();
    }
}
