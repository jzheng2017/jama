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

    public static void main(String[] args) {
        GitRepository migratedProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/easy-mqtt"));
        GitRepository dependencyProject = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/vert.x"));
        Migrator migrator = new Migrator(new File("/home/jiankai/test/" + migratedProject.getLocalPath().getName()));
        migrator.migrate(migratedProject, dependencyProject, "100d6712", "2ba421d6", "5.0.0-SNAPSHOT");
    }
}
