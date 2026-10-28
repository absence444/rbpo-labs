package ru.rbpo.museum.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Long id) {
        super("Не найдено: " + resource + " с id=" + id);
    }
}
