package kz.danke.http.server.exception;

public class MethodInvocationException extends RuntimeException {

    public MethodInvocationException() {
        super();
    }

    public MethodInvocationException(String message) {
        super(message);
    }

    public MethodInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
