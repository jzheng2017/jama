package nl.jiankai.spoon;

import nl.jiankai.api.ProjectQuality;
import nl.jiankai.api.Reference;
import nl.jiankai.api.ReferenceType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SpoonProjectQuality implements ProjectQuality<CtPackage> {

    @Override
    public Set<Reference> getUntestedChanges(Set<Reference> references, CtPackage source) {
        Set<Reference> classes = source.getElements(new TypeFilter<>(CtClass.class)).stream().map(ctClass -> new Reference(ctClass.getQualifiedName(), ReferenceType.CLASS)).collect(Collectors.toSet());
        return references.stream()
                .filter(reference -> reference.referenceType() == ReferenceType.CLASS)
                .filter(reference -> {
                    Reference testClassReference = new Reference(reference.fullyQualified() + "Test", ReferenceType.CLASS);
                    return !classes.contains(testClassReference);
                })
                .collect(Collectors.toSet());
    }
}
