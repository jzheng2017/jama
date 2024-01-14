package nl.jiankai.api;

import java.io.File;

public interface ProjectFactory {
    Project createProject(File directory);
}
