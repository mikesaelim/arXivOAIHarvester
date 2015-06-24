package io.github.mikesaelim.arxivoaiharvester.exception;

public abstract class ArxivException extends RuntimeException {

    public ArxivException() {
        super();
    }

    public ArxivException(String message) {
        super(message);
    }

    public ArxivException(Throwable cause) {
        super(cause);
    }

    public ArxivException(String message, Throwable cause) {
        super(message, cause);
    }

}
