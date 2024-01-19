package nl.jiankai.api;

public record ApiMapping(Element original, Element target, RefactoringType refactoringType, String version) {

    public record Element() {

    }
}
