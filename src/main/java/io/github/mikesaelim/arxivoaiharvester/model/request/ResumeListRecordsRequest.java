package io.github.mikesaelim.arxivoaiharvester.model.request;

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
     * Resumption token returned by the last ListRecords response.
     */
    private final String resumptionToken;

    /**
     * The URI for the request to the repository, created from these settings.
     */
    private final URI uri;

    /**
     * Constructs a ResumeListRecordsRequest object.
     * @throws NullPointerException if resumptionToken is null
     * @throws URISyntaxException if the input did not create a valid URI
     */
    public ResumeListRecordsRequest(@NonNull String resumptionToken) throws URISyntaxException {
        super(Verb.LIST_RECORDS);

        this.resumptionToken = resumptionToken;

        uri = constructURI();
    }

    private URI constructURI() throws URISyntaxException {
        return getUriBuilder()
                .setParameter("resumptionToken", resumptionToken)
                .build();
    }

}