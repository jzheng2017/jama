package nl.jiankai.spoon;

import nl.jiankai.api.Project;
import nl.jiankai.api.Transformer;
import spoon.Launcher;
import spoon.processing.Processor;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static nl.jiankai.spoon.SpoonUtil.*;

public class SpoonTransformer implements Transformer<Processor<?>> {
    private final Project project;
    private final File targetDirectory;
    private final List<Processor<?>> processors = new ArrayList<>();

    public SpoonTransformer(Project project, File targetDirectory) {
        this.project = project;
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
        Launcher launcher = getLauncher(project);
        processors.forEach(launcher::addProcessor);
        launcher.setSourceOutputDirectory(new File(targetDirectory, Paths.get("src", "main", "java").toString()));

        launcher.run();
    }
}
