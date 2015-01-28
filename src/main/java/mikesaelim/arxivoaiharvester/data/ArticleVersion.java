package mikesaelim.arxivoaiharvester.data;

import lombok.Value;
import lombok.experimental.Builder;

import java.time.ZonedDateTime;

/**
 * Data for a version of an article.  Immutable.
 *
 * Created by Mike Saelim on 1/4/15.
 */
@Value
@Builder
public class ArticleVersion {

    /**
     * Numeric version of the article.  Begins at 1.
     */
    private Integer versionNumber;

    /**
     * Submission time of this version.
     */
    private ZonedDateTime submissionTime;

    /**
     * Size of this version, for example, "853kb".
     */
    private String size;

    /**
     * Source type code of this version, for example, "D".
     */
    private String sourceType;

}
