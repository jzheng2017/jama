package nl.jiankai.api.project;


import java.util.List;

public interface GitRepository extends Project {

    default void checkout(String commitId) {
        checkout(commitId, List.of());
    }
    void checkout(String commitId, List<String> path);
}
