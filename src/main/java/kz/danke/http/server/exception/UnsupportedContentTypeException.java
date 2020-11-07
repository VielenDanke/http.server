package kz.danke.http.server.exception;

public class UnsupportedContentTypeException extends RuntimeException {

    public UnsupportedContentTypeException() {
        super();
    }

    public UnsupportedContentTypeException(String message) {
        super(message);
    }

    public UnsupportedContentTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
