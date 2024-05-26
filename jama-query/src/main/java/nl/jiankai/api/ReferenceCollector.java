package nl.jiankai.api;

import java.util.Collection;
import java.util.Set;

public interface ReferenceCollector<R, S> {
    Collection<R> collectReferences(Set<Reference> references, S source);
}
