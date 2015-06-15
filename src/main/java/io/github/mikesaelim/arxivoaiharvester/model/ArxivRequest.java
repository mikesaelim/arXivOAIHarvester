package io.github.mikesaelim.arxivoaiharvester.model;

import lombok.Data;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;

/**
 * Subclasses of this class represent a request to send to the arXiv OAI repository.
 */
@Data
public abstract class ArxivRequest {

    protected static final String SCHEME = "http";
    protected static final String HOST = "export.arxiv.org";
    protected static final String PATH = "/oai2";
    protected static final String METADATA_PREFIX = "arXivRaw";

    protected final Verb verb;

    /**
     * Harvesters will use this to retrieve the constructed URI.
     */
    public abstract URI getUri();

    /**
     * Creates a URIBuilder with all the information for initial request URIs common to all arXiv requests supported by
     * this harvester.  Used in the specific implementations of getUri().
     */
    protected final URIBuilder getUriBuilder() {
        return new URIBuilder()
                .setScheme(SCHEME)
                .setHost(HOST)
                .setPath(PATH)
                .setParameter("verb", verb.getUriFormat());
    }
}
