package kz.danke.http.server.exception;

public class RequestBodyConvertingException extends RuntimeException {

    public RequestBodyConvertingException() {
        super();
    }

    public RequestBodyConvertingException(String message) {
        super(message);
    }

    public RequestBodyConvertingException(String message, Throwable cause) {
        super(message, cause);
    }
}
