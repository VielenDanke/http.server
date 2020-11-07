package kz.danke.http.server.exception;

public class HttpHandlerScanException extends RuntimeException {

    public HttpHandlerScanException(String message) {
        super(message);
    }

    public HttpHandlerScanException(String message, Throwable cause) {
        super(message, cause);
    }
}
