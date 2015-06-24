package io.github.mikesaelim.arxivoaiharvester.exception;

public abstract class ArxivError extends RuntimeException {

    public ArxivError() {
        super();
    }

    public ArxivError(String message) {
        super(message);
    }

    public ArxivError(Throwable cause) {
        super(cause);
    }

    public ArxivError(String message, Throwable cause) {
        super(message, cause);
    }

}
