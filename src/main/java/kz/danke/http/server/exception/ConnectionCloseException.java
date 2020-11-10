package kz.danke.http.server.exception;

public class ConnectionCloseException extends RuntimeException {

    public ConnectionCloseException() {
        super();
    }

    public ConnectionCloseException(String message) {
        super(message);
    }

    public ConnectionCloseException(String message, Throwable cause) {
        super(message, cause);
    }
}
