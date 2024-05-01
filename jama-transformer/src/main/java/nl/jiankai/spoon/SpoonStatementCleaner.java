package nl.jiankai.spoon;

import nl.jiankai.api.ElementTransformationTracker;
import nl.jiankai.api.StatementCleaner;
import nl.jiankai.api.Transformation;
import nl.jiankai.util.TypeUtil;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.code.CtInvocation;

public class SpoonStatementCleaner implements StatementCleaner<Processor<?>> {
    private final ElementTransformationTracker tracker;

    public SpoonStatementCleaner(ElementTransformationTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public Processor<?> removeMethodCall(String methodSignature) {
        return new AbstractProcessor<CtInvocation<?>>() {
            @Override
            public void process(CtInvocation<?> methodCall) {
                String signature = SpoonUtil.getSignatureWithoutClass(methodCall);

                if (signature.equals(methodSignature)) {
                    methodCall.replace(
                            getFactory()
                                    .Code()
                                    .createLiteral(
                                            TypeUtil.getDefaultValue(SpoonUtil.inferMethodCallType(methodCall))
                                    )
                    );
                    tracker.count(new Transformation("Removing method call", methodSignature));
                }
            }
        };
    }
}
