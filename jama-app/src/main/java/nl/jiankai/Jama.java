package nl.jiankai;

import nl.jiankai.api.*;
import nl.jiankai.impl.CompositeProjectFactory;
import nl.jiankai.impl.git.JGitRepositoryFactory;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.SpoonModelBuilder;
import spoon.processing.AbstractProcessor;
import spoon.processing.ProcessingManager;
import spoon.reflect.CtModel;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.NamedElementFilter;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.QueueProcessingManager;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;
import spoon.support.compiler.jdt.JDTSnippetCompiler;

import java.io.File;
import java.util.List;

public class Jama {

    //TODO fix issues
    // rename -> add argument doesn't work as the reference changes mid processing due to renaming
    public static void main(String[] args) {
        GitRepository migratedProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/easy-mqtt"));
        GitRepository dependencyProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/vert.x"));
//        GitRepository dependencyProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/plugin-test-repo-2"));
        Migrator migrator = new Migrator(new File("/home/jiankai/test/" + migratedProject.getLocalPath().getName()));
        migrator.migrate(migratedProject, dependencyProject, "7924f76f", "2ba421d6", "5.0.0-SNAPSHOT");
//        migrator.migrate(migratedProject, dependencyProject, "351af5d9", "0d5c28c7", "1.11.1-SNAPSHOT");
//        migrator.migrate(migratedProject, dependencyProject, "74c3d0d2", "bc08ea14", "1.11.1-SNAPSHOT");
    }
}
