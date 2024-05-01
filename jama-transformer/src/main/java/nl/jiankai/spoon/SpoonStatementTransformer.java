package nl.jiankai.spoon;

import nl.jiankai.api.ElementTransformationTracker;
import nl.jiankai.api.Project;
import nl.jiankai.api.StatementTransformer;
import nl.jiankai.api.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nl.jiankai.spoon.SpoonUtil.getLauncher;

public class SpoonStatementTransformer implements StatementTransformer<Processor<?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpoonStatementTransformer.class);
    private final CtModel dependencyModel;
    private final ElementTransformationTracker tracker;
    public SpoonStatementTransformer(CtModel dependencyModel, ElementTransformationTracker tracker) {
        this.dependencyModel = dependencyModel;
        this.tracker = tracker;
    }

    @Override
    public Processor<?> encapsulateAttribute(String attributeSignature, String methodSignature) {
        return new AbstractProcessor<CtFieldAccess<?>>() {
            @Override
            public void process(CtFieldAccess element) {
                String attributeQualifiedName = element.getVariable().getQualifiedName();
                if (attributeSignature.equals(attributeQualifiedName)) {
                    CtClass<?> methodDeclaredClass = (CtClass<?>) element.getVariable().getFieldDeclaration().getParent();
                    Optional<CtMethod<?>> methodOptional = methodDeclaredClass.getAllMethods().stream().filter(m -> SpoonUtil.getSignature(m).equals(methodSignature)).findFirst();
                    methodOptional.ifPresentOrElse(
                            method -> {
                                LOGGER.info("Encapsulating variable read access '{}' with method '{}'", attributeSignature, methodSignature);
                                element.replace(getFactory().createInvocation(element.getTarget(), method.getReference()));
                                tracker.count(new Transformation("Encapsulate attribute", attributeQualifiedName));
                            },
                            () -> LOGGER.warn("Failed to encapsulate variable access '{}' as method '{}' could not be found", attributeSignature, methodSignature)
                    );
                }
            }
        };
    }

    @Override
    public Processor<?> handleException(String methodSignature, Set<String> exceptions) {
        return new AbstractProcessor<CtInvocation<?>>() {
            @Override
            public void process(CtInvocation<?> methodCall) {
                SpoonUtil.executeIfMethodCallMatches(methodCall, methodSignature, () -> dependencyModel
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

                            CtTry tryElement = getFactory().createTry();
                            tryElement.setBody(parent.clone());
                            CtComment catchBlockComment = getFactory().createInlineComment("handle me");
                            CtBlock<?> catchBlock = getFactory().createCtBlock(catchBlockComment);

                            List<CtTypeReference<? extends Throwable>> alreadyCaughtExceptions = new ArrayList<>();

                            while (!methodExceptions.isEmpty()) {
                                CtTypeReference<? extends Throwable> exception = methodExceptions.poll();
                                boolean canBeCaughtByExceptionHigherInHierarchy = Stream.concat(alreadyCaughtExceptions.stream(), methodExceptions.stream()).anyMatch(e -> e.getActualClass().isAssignableFrom(exception.getActualClass()));

                                if (!canBeCaughtByExceptionHigherInHierarchy) {
                                    CtCatch ctElement = getFactory().createCtCatch("e", exception.getActualClass(), catchBlock);
                                    tryElement.addCatcher(ctElement);
                                    alreadyCaughtExceptions.add(exception);
                                }
                            }

                            parent.replace(tryElement);
                            tracker.count(new Transformation("Handle exception", methodSignature));
                        }));
            }

            private static LinkedList<CtTypeReference<? extends Throwable>> getExceptionsFromMethod(CtMethod method, Set<String> exceptions) {
                return (LinkedList<CtTypeReference<? extends Throwable>>) method
                        .getThrownTypes()
                        .stream()
                        .filter(t -> exceptions.contains(((CtTypeReference<?>) t).getSimpleName()))
                        .collect(Collectors.toCollection(LinkedList::new));
            }
        };
    }
}
