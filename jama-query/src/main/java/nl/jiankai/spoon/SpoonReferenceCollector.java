package nl.jiankai.spoon;

import nl.jiankai.api.Reference;
import nl.jiankai.api.ReferenceCollector;
import nl.jiankai.api.ReferenceType;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.reference.CtReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class SpoonReferenceCollector implements ReferenceCollector<CtReference, CtPackage> {
    @Override
    public Collection<CtReference> collectReferences(Set<Reference> references, CtPackage source) {
        Set<String> referencesString = references.stream().map(Reference::fullyQualified).collect(Collectors.toSet());
        return source
                .getElements(new TypeFilter<>(CtReference.class))
                .stream()
                .filter(reference -> referencesString.contains(reference.getSimpleName()))
                .collect(Collectors.toSet());
    }
}
