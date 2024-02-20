package nl.jiankai.api;

public record Migration(ApiMapping mapping, Migration next) {
    public ApiMapping end() {
        Migration current = this;

        while (current.next != null) {
            current = current.next;
        }

        return current.mapping;
    }
}
