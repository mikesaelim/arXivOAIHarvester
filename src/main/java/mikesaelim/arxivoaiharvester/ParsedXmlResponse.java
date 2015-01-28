package mikesaelim.arxivoaiharvester;

import lombok.Data;
import mikesaelim.arxivoaiharvester.data.ArticleMetadata;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * The information parsed from the XML response from arXiv's OAI repository.
 *
 * Created by Mike Saelim on 1/27/15.
 */
@Data
class ParsedXmlResponse {

    private ZonedDateTime responseDate;

    // TODO: Add error info?

    /**
     * List of records returned by the repository.  It may be empty.
     */
    private List<ArticleMetadata> records;

    /**
     * Resumption token information.
     */
    private String resumptionToken;
    private Integer cursor;
    private Integer completeListSize;

}
