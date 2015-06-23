package io.github.mikesaelim.arxivoaiharvester.model.response;

import io.github.mikesaelim.arxivoaiharvester.model.request.ArxivRequest;

import java.time.ZonedDateTime;

/**
 * Implementations of this interface represent a response from the arXiv OAI repository.
 */
public interface ArxivResponse {

    /**
     * Get the response datetime
     */
    ZonedDateTime getResponseDate();

    /**
     * Get the original request to the harvester
     */
    ArxivRequest getRequest();

}
