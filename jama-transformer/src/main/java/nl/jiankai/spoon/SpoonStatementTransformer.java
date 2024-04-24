package nl.jiankai.spoon;

import nl.jiankai.api.Project;
import nl.jiankai.api.StatementTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static nl.jiankai.spoon.SpoonUtil.getLauncher;

public class SpoonStatementTransformer implements StatementTransformer<Processor<?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpoonStatementTransformer.class);
    private final CtModel dependencyModel;

    public SpoonStatementTransformer(Project dependencyProject) {
        Launcher launcher = getLauncher(dependencyProject);
        launcher.buildModel();
        dependencyModel = launcher.getModel();
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
                            },
                            () -> LOGGER.warn("Failed to encapsulate variable access '{}' as method '{}' could not be found", attributeSignature, methodSignature)
                    );
                }
            }
        };
    }

    @Override
    public Processor<?> handleCheckedException(String methodSignature, Set<String> checkedException) {
        return new AbstractProcessor<CtInvocation<?>>() {
            @Override
            public void process(CtInvocation<?> methodCall) {
//                SpoonUtil.executeIfMethodCallMatches(methodCall, methodSignature, () -> {
//                    dependencyModel
//                            .getElements(new TypeFilter<>(CtMethod.class))
//                            .stream()
//                            .filter(m -> SpoonUtil.getSignature(m).equals(methodSignature))
//                            .findFirst()
//                            .ifPresent(method -> {
//                                List<CtTypeReference<? extends Throwable>> exceptions = method.getThrownTypes().stream().filter(t -> checkedException.contains(((CtTypeReference)t).getSimpleName())).toList();
//                                System.out.println(exceptions);
//                            });
//                });
            }
        };
    }
}
