package dev.mirodil.testing_system.dtos;

public class WrapResponseWithContentKey<T> {
    private T content;

    public WrapResponseWithContentKey(T content) {
        this.content = content;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }
}
