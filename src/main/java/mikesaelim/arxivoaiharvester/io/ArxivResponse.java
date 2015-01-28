package mikesaelim.arxivoaiharvester.io;

import com.google.common.collect.ImmutableList;
import lombok.Value;
import lombok.experimental.Builder;
import mikesaelim.arxivoaiharvester.data.ArticleMetadata;

import java.time.ZonedDateTime;

/**
 * The information returned by a response from arXiv's OAI repository.  Immutable.
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

    // TODO: ERROR INFORMATION

    /**
     * List of records returned by the repository.  It may be empty.  Immutable.
     */
    private ImmutableList<ArticleMetadata> records;

}
