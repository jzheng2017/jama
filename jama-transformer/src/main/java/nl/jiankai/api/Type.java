package nl.jiankai.api;

public record Type(String packagePath, String className) {

    public String fullyQualifiedName() {
        return packagePath + "." + className;
    }
}
