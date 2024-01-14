package nl.jiankai.api;

import java.io.File;

public record ProjectData(ProjectCoordinate coordinate, File pathToProject) {

    @Override
    public String toString() {
        return coordinate.toString();
    }
}
