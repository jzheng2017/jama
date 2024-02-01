package nl.jiankai;

import nl.jiankai.api.GitRepository;
import nl.jiankai.api.MigrationPathEvaluator;
import nl.jiankai.api.Refactoring;
import nl.jiankai.api.RefactoringMiner;
import nl.jiankai.migration.MigrationPathEvaluatorImpl;
import nl.jiankai.refactoringminer.RefactoringMinerImpl;
import nl.jiankai.impl.git.JGitRepositoryFactory;

import java.io.File;
import java.util.Collection;

public class Jama {
    public static void main(String[] args) {
        RefactoringMiner refactoringMiner = new RefactoringMinerImpl();
        GitRepository gitRepository = new JGitRepositoryFactory().createProject(new File("/home/jiankai/IdeaProjects/plugin-test-repo-2"));
        Collection<Refactoring> refactorings = refactoringMiner.detectRefactoringBetweenCommit(gitRepository, "7fdfc95d", "544e1b6c");
        MigrationPathEvaluator migrationPathEvaluator = new MigrationPathEvaluatorImpl();
        migrationPathEvaluator.evaluate(refactorings);
    }
}
