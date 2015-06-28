package io.github.mikesaelim.arxivoaiharvester.exception;

/**
 * Error thrown when the retry loop has been interrupted.
 */
public class InterruptedError extends ArxivError {

    public InterruptedError() {
        super();
    }

    public InterruptedError(String message) {
        super(message);
    }

    public InterruptedError(Throwable cause) {
        super(cause);
    }

    public InterruptedError(String message, Throwable cause) {
        super(message, cause);
    }

}
