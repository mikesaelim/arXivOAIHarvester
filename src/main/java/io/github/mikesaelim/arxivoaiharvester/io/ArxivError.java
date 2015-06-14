package io.github.mikesaelim.arxivoaiharvester.io;

import lombok.Value;

/**
 * Information on what kind of error occurred when the user requested the next batch of records.
 *
 * Created by donerkebab on 1/31/15.
 */
@Value
public class ArxivError {

    public enum Type {
        DEAD_HARVESTER,
        HTTP_ERROR,
        TIME_OUT,
        ILLEGAL_ARGUMENT,
        PARSE_ERROR,
        INTERNAL_ERROR
    }

    private Type errorType;
    private String errorMessage;

}
