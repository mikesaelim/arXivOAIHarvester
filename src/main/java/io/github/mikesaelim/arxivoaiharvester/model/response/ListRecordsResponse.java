package io.github.mikesaelim.arxivoaiharvester.model.response;

import com.google.common.collect.ImmutableList;
import io.github.mikesaelim.arxivoaiharvester.exception.BadResumptionTokenException;
import io.github.mikesaelim.arxivoaiharvester.model.data.ArticleMetadata;
import io.github.mikesaelim.arxivoaiharvester.model.request.ListRecordsRequest;
import io.github.mikesaelim.arxivoaiharvester.model.request.ResumeListRecordsRequest;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.Builder;

import java.net.URISyntaxException;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Builder
public class ListRecordsResponse extends ArxivResponse {

    /**
     * The original request sent to the arXiv OAI repository.
     */
    private ListRecordsRequest request;

    /**
     * Immutable list of records returned by the repository.  It will be empty if no records were found.
     */
    private ImmutableList<ArticleMetadata> records;

    /**
     * Resumption token, if there are more pages left in the response.  If there are no more pages left, this will be
     * null.
     */
    private String resumptionToken;

    /**
     * Position information, if there are more pages left in the response.  If there are no more pages left, this will
     * be null.
     */
    private Integer cursor;
    private Integer completeListSize;

    public ResumeListRecordsRequest resumption() {
        try {
            return new ResumeListRecordsRequest(resumptionToken);
        } catch (URISyntaxException e) {
            throw new BadResumptionTokenException(e);
        }
    }
}
