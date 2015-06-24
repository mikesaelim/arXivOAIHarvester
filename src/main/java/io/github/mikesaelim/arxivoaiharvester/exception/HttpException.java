package io.github.mikesaelim.arxivoaiharvester.exception;

/**
 * Unchecked exception thrown when the harvester has had a problem communicating with the arXiv OAI repository.
 * TODO determine whether this should be checked or unchecked
 */
public class HttpException extends ArxivException {

    public HttpException() {
        super();
    }

    public HttpException(String message) {
        super(message);
    }

    public HttpException(Throwable cause) {
        super(cause);
    }

    public HttpException(String message, Throwable cause) {
        super(message, cause);
    }

}
