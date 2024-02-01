package nl.jiankai.api;

public record Method(String packagePath, String className, String name) {

    public String path() {
        return packagePath + "." + className + "." + name;
    }
}
