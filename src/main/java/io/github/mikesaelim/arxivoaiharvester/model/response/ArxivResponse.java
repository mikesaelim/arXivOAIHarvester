package io.github.mikesaelim.arxivoaiharvester.model.response;

import lombok.Data;

import java.time.ZonedDateTime;

/**
 * Subclasses of this class represent a response from the arXiv OAI repository.
 */
@Data
public abstract class ArxivResponse {

    /**
     * Response datetime.
     */
    private final ZonedDateTime responseDate;

}
