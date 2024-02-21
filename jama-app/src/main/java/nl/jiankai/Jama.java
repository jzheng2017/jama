package nl.jiankai;

import nl.jiankai.api.*;
import nl.jiankai.impl.CompositeProjectFactory;
import nl.jiankai.migration.MigrationPathEvaluatorImpl;
import nl.jiankai.refactoringminer.RefactoringMinerImpl;
import nl.jiankai.impl.git.JGitRepositoryFactory;
import nl.jiankai.spoon.SpoonMethodCallTransformer;
import nl.jiankai.spoon.SpoonMethodQuery;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Jama {
    public static void main(String[] args) {
        GitRepository migratedProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/plugin-test-repo-2"));
        GitRepository dependencyProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/commons-text"));
        Migrator migrator = new Migrator(new File("/home/jiankai/test"));
        migrator.migrate(migratedProject, dependencyProject, "cb85bed", "82aecf3");

//        Project project = new CompositeProjectFactory().createProject(new File("/home/jiankai/IdeaProjects/plugin-test-repo-2"));
//
//        MethodQuery methodQuery = new SpoonMethodQuery(project);
//
//        Optional<Method> method = methodQuery.getMethod(new File("/home/jiankai/IdeaProjects/plugin-test-repo-2/src/main/java/org/example/HelloImpl.java"), new Position(6,0,9,11));
//
//        System.out.println(method.get());
    }
}
