package nl.jiankai;

import nl.jiankai.api.project.GitRepository;
import nl.jiankai.impl.project.git.JGitRepositoryFactory;

import java.io.File;

public class Jama {

    public static void main(String[] args) {
        GitRepository migratedProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/easy-mqtt"));
        GitRepository dependencyProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/vert.x"));
        Migrator migrator = new Migrator(new File("/home/jiankai/test/" + migratedProject.getLocalPath().getName()));
        migrator.migrate(migratedProject, dependencyProject, "100d6712", "2ba421d6", "5.0.0-SNAPSHOT");
    }
}
