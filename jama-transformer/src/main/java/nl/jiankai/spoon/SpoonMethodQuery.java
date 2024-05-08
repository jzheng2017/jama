package nl.jiankai.spoon;

import nl.jiankai.api.*;
import nl.jiankai.api.project.Position;
import nl.jiankai.api.project.Project;
import nl.jiankai.api.project.ProjectType;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;
import java.util.Optional;

public class SpoonMethodQuery implements MethodQuery {
    private final Project project;

    public SpoonMethodQuery(Project project) {
        this.project = project;
    }

    @Override
    public Optional<Method> getMethod(String fullyQualifiedClassName, Position position) {
        CtModel ctModel = getModel();
        Optional<CtMethod<?>> method = ctModel
                .getElements(
                        new TypeFilter<CtMethod<?>>(CtMethod.class) {
                            @Override
                            public boolean matches(CtMethod element) {
                                SourcePosition elementPosition = element.getPosition();
                                return element.getReference().getDeclaringType().getQualifiedName().equals(fullyQualifiedClassName) && elementPosition.getLine() == position.rowStart();
                            }

                        })
                .stream()
                .findFirst();

        return method.map(m -> new Method(getFullyQualifiedName(m)));
    }

    private String getFullyQualifiedName(CtMethod<?> method) {
        return getClass(method) + "." + method.getSimpleName() + "(" + String.join(", ", getArgumentTypes(method)) + ")";
    }

    private List<String> getArgumentTypes(CtMethod<?> method) {
        return method
                .getParameters()
                .stream()
                .filter(argument -> argument.getType() != null)
                .map(argument -> argument.getType().getQualifiedName())
                .toList();
    }

    private CtTypeReference<?> getClass(CtMethod<?> method) {
        return method.getReference().getDeclaringType();
    }

    private CtModel getModel() {
        Launcher launcher = getLauncher();
        launcher.buildModel();
        return launcher.getModel();
    }

    private Launcher getLauncher() {
        return switch (project.getProjectType()) {
            case ProjectType.MAVEN ->
                    new MavenLauncher(project.getLocalPath().getAbsolutePath(), MavenLauncher.SOURCE_TYPE.ALL_SOURCE);
            case UNKNOWN -> throw new UnsupportedOperationException("Unsupported project type");
        };
    }
}
