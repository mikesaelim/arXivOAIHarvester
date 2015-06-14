package io.github.mikesaelim.arxivoaiharvester.model;

import com.google.common.collect.ImmutableList;
import io.github.mikesaelim.arxivoaiharvester.exception.ArxivError;
import io.github.mikesaelim.arxivoaiharvester.model.data.ArticleMetadata;
import lombok.Value;
import lombok.experimental.Builder;

import java.time.ZonedDateTime;

/**
 * The information returned by a response from arXiv's OAI repository.  Immutable.
 *
 * Make sure to check that {@code error == null} before retrieving {@code responseDate} or {@code records}.
 */
@Value
@Builder
public class ArxivResponse {

    /**
     * The original request sent to the arXiv OAI repository.
     */
    private ArxivRequest arxivRequest;

    /**
     * Error information.  If the request executed without errors, this will be null.
     */
    private ArxivError error;

    /**
     * Response datetime.  It may be null if there was an error executing the request.
     */
    private ZonedDateTime responseDate;

    /**
     * List of records returned by the repository.  It may be null if there was an error executing the request.  It will
     * be empty if no records were returned.  Immutable.
     */
    private ImmutableList<ArticleMetadata> records;

}
