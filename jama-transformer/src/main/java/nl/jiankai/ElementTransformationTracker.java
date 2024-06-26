package nl.jiankai;

import nl.jiankai.api.TransformationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ElementTransformationTracker {
    private final Logger LOGGER = LoggerFactory.getLogger(ElementTransformationTracker.class);
    private final Map<TransformationEvent, Long> elementCounter = new ConcurrentHashMap<>();
    private final Map<String, String> mappings = new ConcurrentHashMap<>();
    private final Set<String> affectedFiles = new HashSet<>();
    private final Set<String> affectedMethods = new HashSet<>();

    public void count(TransformationEvent transformationEvent, String filePath) {
        elementCounter.merge(transformationEvent, 1L, Long::sum);
        affectedFiles.add(filePath);
        LOGGER.info("Event {} at {}", transformationEvent, filePath);
    }

    public void countMethod(String methodSignature) {
        affectedMethods.add(methodSignature);
    }

    public void map(String oldSignature, String newSignature) {
        mappings.put(newSignature, oldSignature);
    }

    public String currentSignature(String signature) {
        if (mappings.containsKey(signature)) {
            return currentSignature(mappings.get(signature));
        }

        return signature;
    }

    public Map<TransformationEvent, Long> elementChanges() {
        return new HashMap<>(elementCounter);
    }

    public long changes() {
        return elementCounter
                .values()
                .stream()
                .reduce(Long::sum)
                .orElse(0L);
    }

    public Set<String> affectedMethods() {
        return new HashSet<>(affectedMethods);
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
        mappings.clear();
        affectedMethods.clear();
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
