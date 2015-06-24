package io.github.mikesaelim.arxivoaiharvester.exception;

/**
 * Unchecked exception thrown when the harvester receives a response from the repository with the "badResumptionToken"
 * error condition.
 */
public class BadResumptionTokenException extends ArxivException {

    public BadResumptionTokenException() {
        super();
    }

    public BadResumptionTokenException(String message) {
        super(message);
    }

    public BadResumptionTokenException(Throwable cause) {
        super(cause);
    }

    public BadResumptionTokenException(String message, Throwable cause) {
        super(message, cause);
    }

}
