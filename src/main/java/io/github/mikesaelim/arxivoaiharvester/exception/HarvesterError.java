package io.github.mikesaelim.arxivoaiharvester.exception;

/**
 * Error thrown when the harvester encounters some kind of unrecoverable internal fault.
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
