package kz.danke.http.server.exception;

public class MethodPathVariableNameNotMatchesException extends RuntimeException {

    public MethodPathVariableNameNotMatchesException() {
        super();
    }

    public MethodPathVariableNameNotMatchesException(String message) {
        super(message);
    }

    public MethodPathVariableNameNotMatchesException(String message, Throwable cause) {
        super(message, cause);
    }
}
