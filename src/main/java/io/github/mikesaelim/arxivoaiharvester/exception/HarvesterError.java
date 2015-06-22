package io.github.mikesaelim.arxivoaiharvester.exception;

/**
 * Error thrown when the harvester receives an abnormal response from the repository, or encounters some kind of
 * unrecoverable fault.
 */
public class HarvesterError extends Error {

    public HarvesterError() {
        super();
    }

    public HarvesterError(String message) {
        super(message);
    }

    public HarvesterError(Throwable cause) {
        super(cause);
    }

    public HarvesterError(String message, Throwable cause) {
        super(message, cause);
    }

}
