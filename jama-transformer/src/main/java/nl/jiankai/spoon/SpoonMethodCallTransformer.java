package nl.jiankai.spoon;

import nl.jiankai.api.MethodCallTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.reference.CtTypeReference;

import java.util.Collections;
import java.util.List;

public class SpoonMethodCallTransformer implements MethodCallTransformer<Processor<?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpoonMethodCallTransformer.class);


    @Override
    public Processor<?> rename(String originalSignature, String newName) {
        LOGGER.info("Renaming method name of all methods with signature '{}' to the name '{}'", originalSignature, newName);
        return new AbstractProcessor<CtInvocation<?>>() {
            @Override
            public void process(CtInvocation<?> methodCall) {
                executeIfMethodMatches(methodCall, originalSignature, () -> methodCall.getExecutable().setSimpleName(newName));
            }
        };
    }

    @Override
    public <T> Processor<?> addArgument(String methodSignature, int position, T value) {
        LOGGER.info("Adding a new argument to the method '{}' at position {}", methodSignature, position);

        return new AbstractProcessor<CtInvocation<?>>() {
            @Override
            public void process(CtInvocation<?> methodCall) {
                executeIfMethodMatches(methodCall, methodSignature, () -> {
                    boolean addToEndOfList = position == -1;

                    if (addToEndOfList) {
                        methodCall.addArgument(getFactory().Code().createLiteral(value));

                    } else {
                        methodCall.addArgumentAt(position, getFactory().Code().createLiteral(value));
                    }
                });
            }
        };
    }


    @Override
    public Processor<?> removeArgument(String methodSignature, int position) {
        LOGGER.info("Removing an argument from the method '{}' at position {}", methodSignature, position);

        return new AbstractProcessor<CtInvocation<?>>() {
            @Override
            public void process(CtInvocation<?> methodCall) {
                executeIfMethodMatches(methodCall, methodSignature, () -> methodCall.removeArgument(methodCall.getArguments().get(position)));
            }
        };
    }

    @Override
    public Processor<?> swapArguments(String methodSignature, int positionArgument, int positionArgument2) {
        LOGGER.info("Swapping arguments of method calls to '{}' at position {} and {}", methodSignature, positionArgument, positionArgument2);

        return new AbstractProcessor<CtInvocation<?>>() {
            @Override
            public void process(CtInvocation<?> methodCall) {
                executeIfMethodMatches(methodCall, methodSignature, () -> Collections.swap(methodCall.getArguments(), positionArgument, positionArgument2));
            }
        };
    }

    @Override
    public <T> Processor<?> replaceArgument(String methodSignature, int position, T value) {
        LOGGER.info("Replacing an argument from the method '{}' at position {} with {}", methodSignature, position, value);

        return new AbstractProcessor<CtInvocation<?>>() {
            @Override
            public void process(CtInvocation<?> methodCall) {
                executeIfMethodMatches(methodCall, methodSignature, () -> methodCall.getArguments().get(position).replace(getFactory().Code().createLiteral(value)));
            }
        };
    }


    @Override
    public Processor<?> changeReference(String methodSignature, String newPath) {
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
        return null;
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
        return methodCall.getExecutable().getParameters().stream().map(CtTypeReference::getSimpleName).toList();
    }

    private static CtTypeReference<?> getClass(CtInvocation<?> methodCall) {
        return methodCall.getExecutable().getDeclaringType();
    }
}
