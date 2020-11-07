package kz.danke.http.server.exception;

public class UnsupportedHttpMethodException extends RuntimeException {

    public UnsupportedHttpMethodException() {
        super();
    }

    public UnsupportedHttpMethodException(String message) {
        super(message);
    }

    public UnsupportedHttpMethodException(String message, Throwable cause) {
        super(message, cause);
    }
}
