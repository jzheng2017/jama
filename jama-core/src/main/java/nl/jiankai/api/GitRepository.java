package nl.jiankai.api;


import java.util.List;

public interface GitRepository extends Project {

    default void checkout(String commitId) {
        checkout(commitId, List.of());
    }
    void checkout(String commitId, List<String> path);
}
