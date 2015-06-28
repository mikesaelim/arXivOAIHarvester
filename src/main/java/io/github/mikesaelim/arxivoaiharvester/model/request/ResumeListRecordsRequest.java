package io.github.mikesaelim.arxivoaiharvester.model.request;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A request used to resume a paginated ListRecords request.  Clients generally should not need to construct this
 * directly - in fact, the API is designed such that they do not need to know about the class at all.
 *
 * @see ListRecordsRequest
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ResumeListRecordsRequest extends ListRecordsRequest {

    /**
     * Resumption token returned by the last ListRecords response.
     */
    private final String resumptionToken;

    /**
     * The original request sent to the arXiv OAI repository.
     */
    private final ListRecordsRequest originalRequest;

    /**
     * Constructs a ResumeListRecordsRequest object.
     * @throws NullPointerException if resumptionToken or originalRequest is null
     * @throws URISyntaxException if the input did not create a valid URI
     */
    public ResumeListRecordsRequest(@NonNull String resumptionToken, @NonNull ListRecordsRequest originalRequest)
            throws URISyntaxException {
        super(null, null, null);

        this.resumptionToken = resumptionToken;
        this.originalRequest = originalRequest;

        uri = constructResumptionURI();
    }

    private URI constructResumptionURI() throws URISyntaxException {
        return getUriBuilder()
                .setParameter("resumptionToken", resumptionToken)
                .build();
    }

}