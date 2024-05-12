package nl.jiankai;

import nl.jiankai.api.TransformationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ElementTransformationTracker {
    private final Logger LOGGER = LoggerFactory.getLogger(ElementTransformationTracker.class);
    private final Map<TransformationEvent, Integer> elementCounter = new HashMap<>();

    public void count(TransformationEvent transformationEvent) {
        elementCounter.merge(transformationEvent, 1, Integer::sum);
    }

    public Set<String> affectedClasses() {
        return elementCounter
                .keySet()
                .stream()
                .map(TransformationEvent::element)
                .filter(signature -> !signature.contains("#"))
                .collect(Collectors.toSet());
    }

    public void clear() {
        elementCounter.clear();
    }

    public void report() {
        if (!elementCounter.isEmpty()) {
            LOGGER.info("============ ALL TRANSFORMED ELEMENTS ============");
            elementCounter.forEach((transformation, count) -> LOGGER.info("({}, {}): {}", transformation.element(), transformation.transformation(), count));
            LOGGER.info("===================== END =====================");
        } else {
            LOGGER.info("No elements were transformed");
        }
    }
}
