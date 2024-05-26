package nl.jiankai.spoon;

import com.google.common.io.Files;
import nl.jiankai.api.project.Project;
import nl.jiankai.api.Transformer;
import nl.jiankai.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.processing.Processor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static nl.jiankai.spoon.SpoonUtil.*;

public class SpoonTransformer implements Transformer<Processor<?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpoonTransformer.class);
    private final Project originalProject;
    private final Project transformedProject;
    private final File targetDirectory;
    private final List<Processor<?>> processors = new ArrayList<>();

    public SpoonTransformer(Project transformedProject, File targetDirectory) {
        this(transformedProject, transformedProject, targetDirectory);
    }

    public SpoonTransformer(Project originalProject, Project transformedProject, File targetDirectory) {
        this.originalProject = originalProject;
        this.transformedProject = transformedProject;
        this.targetDirectory = targetDirectory;
    }

    @Override
    public void addProcessor(Processor<?> processor) {
        processors.add(processor);
    }

    @Override
    public void reset() {
        processors.clear();
    }

    @Override
    public void run() {
        Launcher launcher = getLauncher(transformedProject);
        processors.forEach(launcher::addProcessor);
        File processedSourceDirectory = new File(targetDirectory, Paths.get("src", "main", "java").toString());
        launcher.setSourceOutputDirectory(processedSourceDirectory);
        LOGGER.info("Executing {} processors", processors.size());
        launcher.run();
        moveTestClassesFromSourceToTest(processedSourceDirectory);
    }

    private void moveTestClassesFromSourceToTest(File processedSourceDirectory) {
        List<File> sourceDirectories = originalProject.getSourceDirectories().stream().toList();

        if (sourceDirectories.size() != 1) {
            LOGGER.error("Invalid source directories count: {}", sourceDirectories.size());
            throw new IllegalStateException("%s source directories not supported".formatted(sourceDirectories.size()));
        }

        Collection<File> allFilesInProcessedSourceDirectory = FileUtil.findFileRecursive(processedSourceDirectory, "", ".java");
        Set<String> allFilesInOriginalSourceDirectory = FileUtil.findFileRecursive(new File(sourceDirectories.getFirst(), Paths.get("main", "java").toString()), "", ".java").stream().map(this::getPathStartingFromSource).collect(Collectors.toSet());

        if (allFilesInOriginalSourceDirectory.size() == allFilesInProcessedSourceDirectory.size()) {
            return;
        }

        allFilesInProcessedSourceDirectory
                .forEach(file -> {
                    if (!allFilesInOriginalSourceDirectory.contains(getPathStartingFromSource(file))) {
                        moveFileToTestDirectory(file);
                    }
                });
    }

    private void moveFileToTestDirectory(File file) {
        File destination = new File(file.toString().replace("/src/main/java", "/src/test/java"));
        try {
            Files.move(file, destination);
        } catch (IOException e) {
            LOGGER.warn("Could not move {} to {}", file, destination);
        }
    }

    private String getPathStartingFromSource(File file) {
        String path = file.getAbsolutePath();
        int index = path.indexOf("/src/main/java");

        return path.substring(index);
    }
}
