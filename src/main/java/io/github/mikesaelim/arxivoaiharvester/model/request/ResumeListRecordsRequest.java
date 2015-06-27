package io.github.mikesaelim.arxivoaiharvester.model.request;

import io.github.mikesaelim.arxivoaiharvester.model.response.ListRecordsResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A request used to resume a paginated ListRecords request.  Clients generally should not need to construct this
 * directly.
 *
 * @see ListRecordsRequest
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class ResumeListRecordsRequest extends ArxivRequest {

    /**
     * Static dummy value that gets returned when a {@link ListRecordsResponse} has no resumption.
     */
    public static ResumeListRecordsRequest NONE = createNone();


    /**
     * Resumption token returned by the last ListRecords response.
     */
    private final String resumptionToken;

    /**
     * The original request sent to the arXiv OAI repository.
     */
    private final ListRecordsRequest originalRequest;

    /**
     * The URI for the request to the repository, created from these settings.
     */
    private final URI uri;

    /**
     * Constructs a ResumeListRecordsRequest object.
     * @throws NullPointerException if resumptionToken or originalRequest is null
     * @throws URISyntaxException if the input did not create a valid URI
     */
    public ResumeListRecordsRequest(@NonNull String resumptionToken, @NonNull ListRecordsRequest originalRequest)
            throws URISyntaxException {
        super(Verb.LIST_RECORDS);

        this.resumptionToken = resumptionToken;
        this.originalRequest = originalRequest;

        uri = constructURI();
    }

    private URI constructURI() throws URISyntaxException {
        return getUriBuilder()
                .setParameter("resumptionToken", resumptionToken)
                .build();
    }

    private static ResumeListRecordsRequest createNone() {
        try {
            return new ResumeListRecordsRequest("", new ListRecordsRequest(null, null, null));
        } catch (URISyntaxException e) {
            throw new Error("Error creating ResumeListRecordsRequest.NONE");
        }
    }


}