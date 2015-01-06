package mikesaelim.arxivoaiharvester.io;

import lombok.Value;
import lombok.experimental.Builder;
import mikesaelim.arxivoaiharvester.data.ArticleMetadata;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * The information returned by a successful response from arXiv's OAI repository.
 *
 * Created by Mike Saelim on 1/3/15.
 */
@Value
@Builder
public class ArxivResponse {

    /**
     * The original request sent to the arXiv OAI repository.
     */
    private ArxivRequest arxivRequest;

    private ZonedDateTime responseDate;

    /**
     * List of records returned by the repository.  It may be empty.
     */
    private List<ArticleMetadata> records;

    /**
     * Resumption token information.  The user should not need this information - the harvester handles resumption
     * internally - but it is provided here for completeness.
     */
    private String resumptionToken;
    private Integer cursor;
    private Integer completeListSize;

}
