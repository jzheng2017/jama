package nl.jiankai.impl.maven;



import nl.jiankai.api.Project;
import nl.jiankai.api.ProjectFactory;

import java.io.File;

public class MavenProjectFactory implements ProjectFactory {
    @Override
    public Project createProject(File directory) {
        return new MavenProject(directory);
    }
}
