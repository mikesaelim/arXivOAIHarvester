package io.github.mikesaelim.arxivoaiharvester.xml;

import lombok.Data;
import io.github.mikesaelim.arxivoaiharvester.data.ArticleMetadata;
import io.github.mikesaelim.arxivoaiharvester.exception.ArxivError;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * The information parsed from the XML response from arXiv's OAI repository.
 */
@Data
public class ParsedXmlResponse {

    private ZonedDateTime responseDate;

    /**
     * Error information.  If parsing executed without errors, this will be null.
     */
    private ArxivError error;

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