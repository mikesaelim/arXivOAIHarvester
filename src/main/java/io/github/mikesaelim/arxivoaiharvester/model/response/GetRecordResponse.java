package io.github.mikesaelim.arxivoaiharvester.model.response;

import io.github.mikesaelim.arxivoaiharvester.model.data.ArticleMetadata;
import io.github.mikesaelim.arxivoaiharvester.model.request.GetRecordRequest;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class GetRecordResponse implements ArxivResponse {

    /**
     * Response datetime.
     */
    private ZonedDateTime responseDate;

    /**
     * The original request sent to the arXiv OAI repository.
     */
    private GetRecordRequest request;

    /**
     * Record returned by the repository.  It will be null if no record was found.
     */
    private ArticleMetadata record;

}
