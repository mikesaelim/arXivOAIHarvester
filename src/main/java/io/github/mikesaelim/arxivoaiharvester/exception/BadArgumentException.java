package io.github.mikesaelim.arxivoaiharvester.exception;

/**
 * Unchecked exception thrown when the harvester receives a response from the repository with the "badArgument" error
 * condition.
 */
public class BadArgumentException extends ArxivException {

    public BadArgumentException() {
        super();
    }

    public BadArgumentException(String message) {
        super(message);
    }

    public BadArgumentException(Throwable cause) {
        super(cause);
    }

    public BadArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
