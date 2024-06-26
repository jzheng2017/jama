package nl.jiankai.spoon.transformations.method;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Transformation;
import nl.jiankai.api.TransformationEvent;
import nl.jiankai.spoon.SpoonUtil;
import nl.jiankai.util.TypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.Query;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HandleMethodExceptionTransformation implements Transformation<CtInvocation> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandleMethodExceptionTransformation.class);
    private final CtModel dependencyModel;
    private final ElementTransformationTracker tracker;
    private final Set<String> exceptions;
    private final String methodSignature;

    public HandleMethodExceptionTransformation(CtModel dependencyModel, ElementTransformationTracker tracker, Set<String> exceptions, String methodSignature) {
        this.dependencyModel = dependencyModel;
        this.tracker = tracker;
        this.exceptions = exceptions;
        this.methodSignature = methodSignature;
    }

    @Override
    public void apply(CtInvocation methodCall) {
        dependencyModel
                .getElements(new TypeFilter<>(CtMethod.class))
                .stream()
                .filter(m -> SpoonUtil.getSignature(m).equals(methodSignature))
                .findFirst()
                .ifPresent(method -> {
                    LinkedList<CtTypeReference<? extends Throwable>> methodExceptions = getExceptionsFromMethod(method, exceptions);

                    CtStatement parent = methodCall;

                    while (!(parent.getParent() instanceof CtBlock<?>)) {
                        parent = (CtStatement) parent.getParent();
                    }

                    boolean isNewTry = false;

                    CtTry tryElement;
                    final boolean isInsideTryBlock = parent.getParent().getParent() instanceof CtTry;

                    if (isInsideTryBlock) {
                        tryElement = (CtTry) parent.getParent().getParent();
                    } else {
                        isNewTry = true;
                        tryElement = methodCall.getFactory().createTry();
                        if (parent instanceof CtLocalVariable<?> variableDeclaration) {
                            CtLocalVariable cloned = variableDeclaration.clone();
                            CtLiteral<Object> initializationValue = methodCall
                                    .getFactory()
                                    .Code()
                                    .createLiteral(
                                            TypeUtil.getDefaultValue(variableDeclaration.getAssignment().getType().getSimpleName())
                                    );
                            cloned.setAssignment(initializationValue);
                            parent.insertBefore(cloned);
                            CtAssignment<?, ?> assignment = methodCall.getFactory().createVariableAssignment(cloned.getReference(), cloned.isStatic(), variableDeclaration.getAssignment());
                            tryElement.setBody(assignment);
                        } else if (parent instanceof CtAssignment<?, ?> || parent instanceof CtInvocation<?>) {
                            tryElement.setBody(parent.clone());
                        } else {
                            LOGGER.warn("Handling a thrown exception of non-method types is not supported! Type encountered: {}", parent);
                            throw new IllegalStateException();
                        }
                    }

                    addNewCatchBlocks(methodCall, tryElement, methodExceptions);

                    if (isNewTry) {
                        parent.replace(tryElement);
                    }

                    tracker.count(new TransformationEvent("Handle exception", methodSignature), methodCall.getPosition().getFile().getAbsolutePath());
                });
    }

    private void addNewCatchBlocks(CtInvocation methodCall, CtTry tryElement, LinkedList<CtTypeReference<? extends Throwable>> methodExceptions) {
        CtComment catchBlockComment = methodCall.getFactory().createInlineComment("handle me");
        CtBlock<?> catchBlock = methodCall.getFactory().createCtBlock(catchBlockComment);

        List<CtTypeReference<? extends Throwable>> alreadyCaughtExceptions = new ArrayList<>(tryElement.getCatchers().stream().map(catcher -> catcher.getParameter().getType()).toList());

        while (!methodExceptions.isEmpty()) {
            CtTypeReference<? extends Throwable> exception = methodExceptions.poll();
            boolean canBeCaughtByExceptionHigherInHierarchy = Stream.concat(alreadyCaughtExceptions.stream(), methodExceptions.stream()).anyMatch(e -> e.getActualClass().isAssignableFrom(exception.getActualClass()));

            if (!canBeCaughtByExceptionHigherInHierarchy) {
                CtCatch ctElement = methodCall.getFactory().createCtCatch("e", exception.getActualClass(), catchBlock);
                tryElement.addCatcher(ctElement);
                alreadyCaughtExceptions.add(exception);
            }
        }
    }

    private LinkedList<CtTypeReference<? extends Throwable>> getExceptionsFromMethod(CtMethod method, Set<String> exceptions) {
        return (LinkedList<CtTypeReference<? extends Throwable>>) method
                .getThrownTypes()
                .stream()
                .filter(t -> exceptions.contains(((CtTypeReference<?>) t).getSimpleName()))
                .collect(Collectors.toCollection(LinkedList::new));
    }
}
