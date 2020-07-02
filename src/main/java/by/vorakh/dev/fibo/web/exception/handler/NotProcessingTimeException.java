package by.vorakh.dev.fibo.web.exception.handler;

public class NotProcessingTimeException extends RuntimeException {

    public NotProcessingTimeException() {

    }

    public NotProcessingTimeException(String message) {

        super(message);
    }

    public NotProcessingTimeException(String message, Throwable cause) {

        super(message, cause);
    }
}
