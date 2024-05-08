package nl.jiankai.api.project;

import java.io.File;

public interface ProjectFactory {
    Project createProject(File directory);
}
