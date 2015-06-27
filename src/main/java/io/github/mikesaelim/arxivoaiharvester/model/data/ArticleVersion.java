package io.github.mikesaelim.arxivoaiharvester.model.data;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

/**
 * Data for a version of an article.  Immutable.
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
