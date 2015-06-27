package io.github.mikesaelim.arxivoaiharvester.model.response;

import com.google.common.collect.ImmutableList;
import io.github.mikesaelim.arxivoaiharvester.exception.BadResumptionTokenException;
import io.github.mikesaelim.arxivoaiharvester.model.data.ArticleMetadata;
import io.github.mikesaelim.arxivoaiharvester.model.request.ListRecordsRequest;
import io.github.mikesaelim.arxivoaiharvester.model.request.ResumeListRecordsRequest;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;

@Value
@Builder
public class ListRecordsResponse implements ArxivResponse {

    /**
     * Response datetime.
     */
    private ZonedDateTime responseDate;

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
    private BigInteger cursor;
    private BigInteger completeListSize;

    /**
     * Create a {@link ResumeListRecordsRequest} that resumes this request if there are more pages left in the response.
     * If there are no more pages left, this will return {@link ResumeListRecordsRequest#NONE}.
     */
    public ResumeListRecordsRequest resumption() {
        if (resumptionToken == null) {
            return ResumeListRecordsRequest.NONE;
        }

        try {
            return new ResumeListRecordsRequest(resumptionToken, request);
        } catch (URISyntaxException e) {
            throw new BadResumptionTokenException(e);
        }
    }
}
