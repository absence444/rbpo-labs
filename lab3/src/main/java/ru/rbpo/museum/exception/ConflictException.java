package ru.rbpo.museum.exception;

/** Нарушение бизнес-правила, связанное с текущим состоянием данных. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
