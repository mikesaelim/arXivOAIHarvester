package io.github.mikesaelim.arxivoaiharvester.model.request;

import lombok.*;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A GetRecord request, used to retrieve a single record from the repository by its identifier.
 *
 * arXiv identifiers follow the format "oai:arXiv.org:nucl-ex/0511023", or "oai:arXiv.org:1302.2146", or
 * "oai:arXiv.org:1501.00001".  However, the constructor will also accept identifiers without the "oai:arXiv.org"
 * prefix, and convert them automatically.
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class GetRecordRequest extends ArxivRequest {

    /**
     * Unique identifier of the record.  For records in the arXiv OAI repository, this starts with "oai:arXiv.org:".
     */
    private final String identifier;

    /**
     * The URI for the request to the repository, created from these settings.
     */
    private final URI uri;

    /**
     * Constructs a GetRecordRequest object.
     * @param identifier unique record identifier, with or without the "oai:arXiv.org:" prefix.
     * @throws URISyntaxException if the input did not create a valid URI
     */
    public GetRecordRequest(@NonNull String identifier) throws URISyntaxException {
        super(Verb.GET_RECORD);

        if (identifier.startsWith("oai:arXiv.org:")) {
            this.identifier = identifier;
        } else {
            this.identifier = "oai:arXiv.org:" + identifier;
        }

        uri = constructURI();
    }

    private URI constructURI() throws URISyntaxException {
        return getUriBuilder()
                .setParameter("metadataPrefix", METADATA_PREFIX)
                .setParameter("identifier", identifier)
                .build();
    }

}
