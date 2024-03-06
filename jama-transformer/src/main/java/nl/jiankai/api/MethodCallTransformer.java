package nl.jiankai.api;

public interface MethodCallTransformer extends Transformer {
    /**
     * Renames the method name of all method calls matching the provided signature to a new name
     * @param originalSignature the signature of the original method that is being called
     * @param newName the name method name to refactor to
     */
    void rename(String originalSignature, String newName);

    /**
     * Add an argument to the method call matching the fully qualified method name. If no default value is provided it will use the default value of the primitive type or null in case of an object.
     * @param methodSignature signature of the method call to be refactored
     * @param position position of the argument
     * @param value a value to insert in the method call for the added argument
     */
    <T> void addArgument(String methodSignature, int position, T value);

    default void addArgument(String methodSignature, int position) {
        addArgument(methodSignature, position, null);
    }

    /**
     * Remove an argument of all matching methods called at a certain position
     * @param methodSignature signature of the method call
     * @param position position of the argument in the method call
     */
    void removeArgument(String methodSignature, int position);

    /**
     * Swap the positions of two arguments
     * @param methodSignature signature of the method call
     * @param positionArgument position of the first argument
     * @param positionArgument2 position of the second argument
     */
    void swapArguments(String methodSignature, int positionArgument, int positionArgument2);

    /**
     * Change the reference of the method calls matching the provided signature (e.g. org.example.Class#function to org.exampleV2.Class#function)
     * @param methodSignature the signature of the original method that is being called
     * @param newPath the path to the new function (e.g. org.exampleV2.Class)
     */
    void changeReference(String methodSignature, String newPath);
}
