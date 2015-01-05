package mikesaelim.arxivoaiharvester.io;

import lombok.Value;
import mikesaelim.arxivoaiharvester.data.ArticleMetadata;

import java.util.List;

/**
 * The information returned by a successful response from arXiv's OAI repository.
 *
 * Created by Mike Saelim on 1/3/15.
 */
@Value
public class ArxivResponse {

    /**
     * List of records returned by the repository.  It may be empty.
     */
    private List<ArticleMetadata> records;

}
