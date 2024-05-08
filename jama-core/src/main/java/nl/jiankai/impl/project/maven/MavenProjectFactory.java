package nl.jiankai.impl.project.maven;



import nl.jiankai.api.project.Project;
import nl.jiankai.api.project.ProjectFactory;

import java.io.File;

public class MavenProjectFactory implements ProjectFactory {
    @Override
    public Project createProject(File directory) {
        return new MavenProject(directory);
    }
}
