package nl.jiankai.spoon;

import nl.jiankai.api.Project;
import nl.jiankai.api.ProjectType;
import nl.jiankai.api.Transformer;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.processing.Processor;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SpoonTransformer implements Transformer<Processor<?>> {
    private final Project project;
    private final File targetDirectory;
    private final List<Processor<?>> processors = new ArrayList<>();

    public SpoonTransformer(Project project, File targetDirectory) {
        this.project = project;
        this.targetDirectory = targetDirectory;
    }


    private Launcher getLauncher() {
        return switch (project.getProjectType()) {
            case ProjectType.MAVEN ->
                    new MavenLauncher(project.getLocalPath().getAbsolutePath(), MavenLauncher.SOURCE_TYPE.ALL_SOURCE);
            case UNKNOWN -> throw new UnsupportedOperationException("Unsupported project type");
        };
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
        Launcher launcher = getLauncher();
        processors.forEach(launcher::addProcessor);
        launcher.setSourceOutputDirectory(new File(targetDirectory, Paths.get("src", "main", "java").toString()));
        launcher.getEnvironment()
                .setPrettyPrinterCreator(() -> {
                    DefaultJavaPrettyPrinter printer = new DefaultJavaPrettyPrinter(launcher.getEnvironment());
                    printer.setIgnoreImplicit(false);
                    return printer;
                });
        launcher.getEnvironment().setNoClasspath(true); //which allows us to suppress compilation errors (no exception thrown)
        launcher.run();
    }
}
