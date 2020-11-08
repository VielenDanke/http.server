package kz.danke.http.server.exception;

public class ArgsMismatchException extends RuntimeException {

    public ArgsMismatchException() {
        super();
    }

    public ArgsMismatchException(String message) {
        super(message);
    }
}
