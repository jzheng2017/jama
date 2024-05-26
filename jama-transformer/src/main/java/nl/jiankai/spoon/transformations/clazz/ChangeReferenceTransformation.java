package nl.jiankai.spoon.transformations.clazz;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Transformation;
import nl.jiankai.api.TransformationEvent;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

public class ChangeReferenceTransformation implements Transformation<CtTypeReference> {
    private final ElementTransformationTracker tracker;
    private final String newSignature;
    private final Factory dependencyFactory;

    public ChangeReferenceTransformation(ElementTransformationTracker tracker, String newSignature, Factory dependencyFactory) {
        this.tracker = tracker;
        this.newSignature = newSignature;
        this.dependencyFactory = dependencyFactory;
    }

    @Override
    public void apply(CtTypeReference reference) {
        CtTypeReference<?> newReference =
                dependencyFactory
                        .Type()
                        .get(newSignature)
                        .getReference();
        String oldSignature = reference.getQualifiedName();
        String path = getPath(reference);
        reference.replace(newReference);

        tracker.count(new TransformationEvent("Changing reference", newSignature), path);
        tracker.map(oldSignature, newSignature);
    }

    private String getPath(CtElement element) {
        if (element == null) {
            return "";
        }

        if (element.getPosition().isValidPosition()) {
            return element.getPosition().getFile().getAbsolutePath();
        } else {
            return getPath(element.getParent());
        }
    }
}
