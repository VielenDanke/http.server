package kz.danke.http.server.exception;

public class BodyNotFoundException extends RuntimeException {

    public BodyNotFoundException() {
        super();
    }

    public BodyNotFoundException(String message) {
        super(message);
    }

    public BodyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
