package nl.jiankai.api;


import java.util.Collection;
import java.util.Set;

public interface RefactoringMiner {

    Collection<Refactoring> detectRefactoringBetweenCommit(GitRepository gitRepository, String startCommitId, String endCommitId, Set<RefactoringType> refactoringTypes);
    default Collection<Refactoring> detectRefactoringBetweenCommit(GitRepository gitRepository, String startCommitId, String endCommitId) {
        return detectRefactoringBetweenCommit(gitRepository, startCommitId, endCommitId, Set.of());
    }

}
