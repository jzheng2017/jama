package nl.jiankai;

import nl.jiankai.api.project.GitRepository;
import nl.jiankai.impl.project.git.JGitRepositoryFactory;
import nl.jiankai.util.FileUtil;

import java.io.File;
import java.util.Collection;

public class Jama {

    public static void main(String[] args) {
//        GitRepository migratedProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/opencsv-source"));
//        GitRepository dependencyProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/commons-collections"));
//        Migrator migrator = new Migrator(new File("/home/jiankai/test/" + migratedProject.getLocalPath().getName()));
//        migrator.migrate(migratedProject, dependencyProject, "ebdf764", "1dc530e6", "4.5.0");

                GitRepository migratedProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/plugin-test-repo-2"));
        GitRepository dependencyProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/commons-text"));
        Migrator migrator = new Migrator(new File("/home/jiankai/test/" + migratedProject.getLocalPath().getName()));
        migrator.migrate(migratedProject, dependencyProject, "82aecf36", "5303b7d8", "1.11.1-SNAPSHOT");
    }
}
