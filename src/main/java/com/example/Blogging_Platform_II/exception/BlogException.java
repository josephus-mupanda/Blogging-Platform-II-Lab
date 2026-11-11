package com.example.Blogging_Platform_II.exception;

/** One exception for the business rules, with a code the controllers turn into a status. */
public class BlogException extends RuntimeException {
    public enum Code { BAD_REQUEST, NOT_FOUND, CONFLICT }

    private final Code code;

    private BlogException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }

    public static BlogException badRequest(String message) {
        return new BlogException(Code.BAD_REQUEST, message);
    }

    public static BlogException notFound(String resource) {
        return new BlogException(Code.NOT_FOUND, resource + " was not found.");
    }

    public static BlogException conflict(String message) {
        return new BlogException(Code.CONFLICT, message);
    }

    public static BlogException duplicateValue() {
        return conflict("A record already uses this value.");
    }
}
