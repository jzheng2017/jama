package nl.jiankai.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ElementTransformationTracker {
    private final Logger LOGGER = LoggerFactory.getLogger(ElementTransformationTracker.class);
    private final Map<Transformation, Integer> elementCounter = new HashMap<>();

    public void count(Transformation transformation) {
        elementCounter.merge(transformation, 1, Integer::sum);
    }

    public void report() {
        LOGGER.info("============ ALL TRANSFORMED ELEMENTS ============");
        elementCounter.forEach((transformation, count) -> LOGGER.info("({}, {}): {}", transformation.element(), transformation.transformation(), count));
        LOGGER.info("============ END ============");
        elementCounter.clear();
    }
}
