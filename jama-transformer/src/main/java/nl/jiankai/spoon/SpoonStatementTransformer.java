package nl.jiankai.spoon;

import nl.jiankai.api.StatementTransformer;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.FieldAccessFilter;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.Optional;

public class SpoonStatementTransformer implements StatementTransformer<Processor<?>> {


    @Override
    public Processor<?> encapsulateAttribute(String attributeSignature, String methodSignature) {
        return new AbstractProcessor<CtFieldAccess<?>>() {
            @Override
            public void process(CtFieldAccess element) {
                String attributeQualifiedName = element.getVariable().getQualifiedName();
                if (attributeSignature.equals(attributeQualifiedName)) {
                    System.out.println();
//                        CtExpression<?> target = getFactory().createThisAccess(method.get().getDeclaringType().getReference());
//                        element.replace(getFactory().createInvocation(target, method.get().getReference()));
                }
            }
        };
    }
}
