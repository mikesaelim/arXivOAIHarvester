package io.github.mikesaelim.arxivoaiharvester.exception;

/**
 * Unchecked exception thrown when the harvester receives a 302 Redirect response from the repository.  Handling this
 * response is not currently supported.
 */
public class UnsupportedRedirectException extends RuntimeException {

    public UnsupportedRedirectException() {
        super();
    }

    public UnsupportedRedirectException(String message) {
        super(message);
    }

    public UnsupportedRedirectException(Throwable cause) {
        super(cause);
    }

    public UnsupportedRedirectException(String message, Throwable cause) {
        super(message, cause);
    }

}
