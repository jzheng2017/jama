package nl.jiankai.spoon;

import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtTypeReference;

import java.util.List;

public class SpoonUtil {

    public static String getSignature(CtMethod<?> method) {
        return getClass(method) + "#" + method.getReference().getSimpleName() + "(" + String.join(", ", getArgumentTypes(method)) + ")";
    }

    public static String getSignature(CtInvocation<?> methodCall) {
        return getClass(methodCall) + "." + methodCall.getExecutable().getSimpleName() + "(" + String.join(", ", getArgumentTypes(methodCall)) + ")";
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
}
