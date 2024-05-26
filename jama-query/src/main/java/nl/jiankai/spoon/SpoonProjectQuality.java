package nl.jiankai.spoon;

import nl.jiankai.api.ProjectQuality;
import nl.jiankai.api.Reference;
import nl.jiankai.api.ReferenceCollector;
import nl.jiankai.api.ReferenceType;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.reference.CtReference;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SpoonProjectQuality implements ProjectQuality<CtPackage> {
    private final ReferenceCollector<CtReference, CtPackage> referenceCollector;

    public SpoonProjectQuality(ReferenceCollector<CtReference, CtPackage> referenceCollector) {
        this.referenceCollector = referenceCollector;
    }

    @Override
    public Set<Reference> getUntestedChanges(Set<Reference> references, CtPackage source) {
        Set<CtReference> spoonReferences = new HashSet<>(referenceCollector.collectReferences(references, source));
        Set<Reference> methodReferences = references.stream().filter(reference -> reference.referenceType().equals(ReferenceType.METHOD)).collect(Collectors.toSet());

        return Set.of();
    }
}
