package com.assignment.dispatch.exception;

/** Thrown for semantically invalid input that bean validation cannot catch (e.g. duplicate IDs in one batch). */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
