package nl.jiankai.spoon;

import nl.jiankai.api.StatementTransformer;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.code.CtFieldAccess;

public class SpoonStatementTransformer implements StatementTransformer<Processor<?>> {


    @Override
    public Processor<?> encapsulateAttribute(String attributeSignature, String methodSignature) {
        return new AbstractProcessor<CtFieldAccess<?>>() {

            @Override
            public void process(CtFieldAccess element) {
                String attributeQualifiedName = element.getVariable().getQualifiedName();
                if (attributeSignature.equals(attributeQualifiedName)) {

                }
            }
        };
    }
}
