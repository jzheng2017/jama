package nl.jiankai.spoon;

import nl.jiankai.api.ProjectQuality;
import nl.jiankai.api.Reference;
import nl.jiankai.api.ReferenceType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SpoonProjectQuality implements ProjectQuality<CtPackage> {

    @Override
    public Set<Reference> getUntestedChanges(Set<Reference> references, CtPackage source) {
        List<CtClass> classes = source.getElements(new TypeFilter<>(CtClass.class)).stream().toList();
        return references.stream()
                .filter(reference -> reference.referenceType() == ReferenceType.CLASS)
                .filter(reference -> {
                    Reference testClassReference = new Reference(reference.fullyQualified() + "Test", ReferenceType.CLASS);
                    return !classes.contains(testClassReference);
                })
                .collect(Collectors.toSet());
    }
}
