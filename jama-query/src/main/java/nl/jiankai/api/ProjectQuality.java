package nl.jiankai.api;

import java.util.Set;

public interface ProjectQuality<S> {
    Set<Reference> getUntestedChanges(Set<Reference> references, S source);
}
