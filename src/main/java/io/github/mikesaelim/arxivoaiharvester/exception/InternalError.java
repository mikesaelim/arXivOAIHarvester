package io.github.mikesaelim.arxivoaiharvester.exception;

/**
 * Error thrown when the harvester receives an abnormal response from the repository, or encounters some kind of
 * unrecoverable fault.
 */
public class InternalError extends Error {

    public InternalError() {
        super();
    }

    public InternalError(String message) {
        super(message);
    }

    public InternalError(Throwable cause) {
        super(cause);
    }

    public InternalError(String message, Throwable cause) {
        super(message, cause);
    }

}
