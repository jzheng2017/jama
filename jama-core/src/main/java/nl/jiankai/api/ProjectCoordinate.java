package nl.jiankai.api;

public record ProjectCoordinate(String groupId, String artifactId, String version) {

    @Override
    public String toString() {
        return groupId + "-" + artifactId + "-" + version;
    }
}
