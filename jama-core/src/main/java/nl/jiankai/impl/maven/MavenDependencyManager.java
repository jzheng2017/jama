package nl.jiankai.impl.maven;

import nl.jiankai.api.Dependency;
import nl.jiankai.api.ProjectDependencyManager;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

public class MavenDependencyManager implements ProjectDependencyManager {
    private final File pomFile;

    public MavenDependencyManager(File pomFile) {
        this.pomFile = pomFile;
    }

    @Override
    public void changeDependency(Dependency dependency) {
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileInputStream(pomFile));
            Stream<org.apache.maven.model.Dependency> dependencyManagement = Stream.empty();
            if (model.getDependencyManagement() != null) {
                dependencyManagement = model.getDependencyManagement().getDependencies().stream();
            }

            org.apache.maven.model.Dependency originalDependency = Stream
                    .concat(model.getDependencies().stream(), dependencyManagement)
                    .filter(dep -> Objects.equals(dep.getGroupId(), dependency.groupId()) && Objects.equals(dep.getArtifactId(), dependency.artifactId()))
                    .findAny()
                    .orElseThrow(() -> new NoSuchElementException("Could not find dependency %s:%s".formatted(dependency.groupId(), dependency.artifactId())));

            String version = originalDependency.getVersion();

            if (version.startsWith("$")) {
                String key = version.substring(2, version.length() - 1);
                model.getProperties().setProperty(key, dependency.version());
            } else {
                originalDependency.setVersion(dependency.version());
            }

            MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(new FileOutputStream(pomFile), model);
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }
}
