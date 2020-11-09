package kz.danke.http.server.exception;

public class StaticElementException extends RuntimeException {

    public StaticElementException() {
        super();
    }

    public StaticElementException(String message) {
        super(message);
    }

    public StaticElementException(String message, Throwable cause) {
        super(message, cause);
    }
}
