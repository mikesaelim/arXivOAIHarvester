package io.github.mikesaelim.arxivoaiharvester.io;

import lombok.Getter;
import lombok.NonNull;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * An ArxivRequest for the GetRecord verb.  This request is used to retrieve a single record from the repository, by its
 * identifier.
 *
 * arXiv identifiers follow the format "oai:arXiv.org:nucl-ex/0511023", or "oai:arXiv.org:1302.2146", or
 * "oai:arXiv.org:1501.00001".  However, the constructor will also accept identifiers without the "oai:arXiv.org"
 * prefix, and convert them automatically.
 */
@Getter
public class ArxivGetRecordRequest extends ArxivRequest {

    /**
     * Unique identifier of the record.  For records in the arXiv OAI repository, this should start with "oai:arXiv.org:".
     */
    private final String identifier;

    /**
     * The URI for the initial request to the repository, created from these settings.
     */
    private final URI initialUri;

    /**
     * Constructs an ArxivGetRecordRequest object.
     * @param identifier unique record identifier, with or without the "oai:arXiv.org:" prefix.
     * @throws URISyntaxException if the input did not create a valid initial URI
     */
    public ArxivGetRecordRequest(@NonNull String identifier) throws URISyntaxException {
        super(Verb.GET_RECORD);

        if (identifier.startsWith("oai:arXiv.org:")) {
            this.identifier = identifier;
        } else {
            this.identifier = "oai:arXiv.org:" + identifier;
        }

        initialUri = constructInitialURI();
    }

    private URI constructInitialURI() throws URISyntaxException {
        return getInitialUriBuilder()
                .setParameter("identifier", identifier)
                .build();
    }

}
