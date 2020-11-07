package kz.danke.http.server.exception;

public class PathConflictException extends RuntimeException {
    public PathConflictException(String message) {
        super(message);
    }

    public PathConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
