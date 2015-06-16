package io.github.mikesaelim.arxivoaiharvester.exception;

/**
 * Unchecked exception thrown when the harvester has received 503 Retry After responses from the repository that have
 * exceeded the allowable wait time.
 * TODO determine whether this should be checked or unchecked
 */
public class TimeoutException extends RuntimeException {

    public TimeoutException() {
        super();
    }

    public TimeoutException(String message) {
        super(message);
    }

    public TimeoutException(Throwable cause) {
        super(cause);
    }

    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

}
