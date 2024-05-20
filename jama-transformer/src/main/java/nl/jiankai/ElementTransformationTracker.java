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
    private final Set<String> affectedFiles = new HashSet<>();

    public void count(TransformationEvent transformationEvent, String filePath) {
        elementCounter.merge(transformationEvent, 1, Integer::sum);
        affectedFiles.add(filePath);
    }

    public Set<String> affectedClasses() {
        return affectedFiles
                .stream()
                .filter(filePath -> filePath.contains("/src/main/java"))
                .map(filePath -> {
                    String noJavaExtensionPath = filePath.replace(".java", "");
                    String noSrcJava = noJavaExtensionPath.substring(noJavaExtensionPath.lastIndexOf("/src/main/java/") + "/src/main/java/".length());
                    return noSrcJava.replaceAll("/", ".");
                })
                .collect(Collectors.toSet());
    }

    public void clear() {
        elementCounter.clear();
        affectedFiles.clear();
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
