package io.github.mikesaelim.arxivoaiharvester.model.response;

import io.github.mikesaelim.arxivoaiharvester.model.data.ArticleMetadata;
import io.github.mikesaelim.arxivoaiharvester.model.request.GetRecordRequest;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.Builder;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Builder
public class GetRecordResponse extends ArxivResponse {

    /**
     * The original request sent to the arXiv OAI repository.
     */
    private GetRecordRequest request;

    /**
     * Record returned by the repository.  It will be null if no record was found.
     */
    private ArticleMetadata record;

}
