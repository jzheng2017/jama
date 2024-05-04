package nl.jiankai.operators;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Migration;
import nl.jiankai.api.TransformationProvider;
import nl.jiankai.spoon.transformations.method.HandleMethodExceptionTransformation;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;

import java.util.Set;
import java.util.stream.Collectors;

public class MethodExceptionOperator<P> implements MigrationOperator{
    private final TransformationProvider<CtInvocation> transformationProvider;
    private final ElementTransformationTracker tracker;
    private final CtModel dependencyModel;
    public MethodExceptionOperator(TransformationProvider<CtInvocation> transformationProvider, ElementTransformationTracker tracker, CtModel dependencyModel) {
        this.transformationProvider = transformationProvider;
        this.tracker = tracker;
        this.dependencyModel = dependencyModel;
    }

    @Override
    public void migrate(Migration migration) {
        String methodSignature = migration.end().target().signature();
        Set<String> exceptions = migration.getContext("exception").stream().map(String.class::cast).collect(Collectors.toSet());
        transformationProvider.produce(migration.mapping().original().signature(), new HandleMethodExceptionTransformation(dependencyModel, tracker, exceptions, methodSignature));
//        transformer.addProcessor(statementTransformer.handleException("org.apache.commons.text.WordUtils#capitalizeV3(String)", migration.getContext("exception").stream().map(String.class::cast).collect(Collectors.toSet())));
    }
}
