package nl.jiankai.api.project;

import java.io.File;

public record ProjectData(ProjectCoordinate coordinate, File pathToProject) {

    @Override
    public String toString() {
        return coordinate.toString();
    }
}
