package io.github.mikesaelim.arxivoaiharvester.exception;

/**
 * Error thrown when the harvester encounters an invalid response from the repository.
 */
public class RepositoryError extends Error {

    public RepositoryError() {
        super();
    }

    public RepositoryError(String message) {
        super(message);
    }

    public RepositoryError(Throwable cause) {
        super(cause);
    }

    public RepositoryError(String message, Throwable cause) {
        super(message, cause);
    }

}
