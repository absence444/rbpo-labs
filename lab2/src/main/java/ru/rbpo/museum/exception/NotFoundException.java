package ru.rbpo.museum.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Long id) {
        super(resource + " с id=" + id + " не найден(а)");
    }
}
