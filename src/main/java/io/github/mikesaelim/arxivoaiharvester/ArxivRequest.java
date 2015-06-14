package io.github.mikesaelim.arxivoaiharvester;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * The complete set of information needed to construct a request to send to the arXiv OAI repository.  There are different
 * implementations of this class for different request verbs.
 */
@Getter
@Setter
public abstract class ArxivRequest {

    protected final String SCHEME = "http";
    protected final String HOST = "export.arxiv.org";
    protected final String PATH = "/oai2";
    protected final String METADATA_PREFIX = "arXivRaw";

    private final Verb verb;

    /**
     * "User-Agent" HTTP header, which should contain information about the user agent originating the request.  For
     * example, "Mike's arXiv database duplicator/0.1".
     */
    private String userAgentHeader;
    /**
     * "From" HTTP header, which should contain the email address of the person controlling the harvester.
     */
    private String fromHeader;

    public ArxivRequest(Verb verb) {
        this.verb = verb;
    }

    /**
     * Generate the initial request URI.
     * @return initial request URI
     */
    public abstract URI getInitialUri();

    /**
     * Generate a subsequent URI that resumes a request.
     * @param resumptionToken resumption token included at the end of the last response from the arXiv OAI repository
     * @return resumption request URI
     * @throws java.lang.NullPointerException if resumptionToken is null
     * @throws URISyntaxException if resumptionToken makes an invalid URI
     */
    public URI getResumptionURI(@NonNull String resumptionToken) throws URISyntaxException {
        return new URIBuilder()
                .setScheme(SCHEME)
                .setHost(HOST)
                .setPath(PATH)
                .setParameter("verb", this.getVerb().getUriFormat())
                .setParameter("resumptionToken", resumptionToken)
                .build();
    }

    /**
     * Creates a URIBuilder with all the information for initial request URIs common to all arXiv requests supported by
     * this harvester.  Used in the specific implementations of getInitialUri().
     */
    protected final URIBuilder getInitialUriBuilder() {
        return new URIBuilder()
                .setScheme(SCHEME)
                .setHost(HOST)
                .setPath(PATH)
                .setParameter("verb", this.getVerb().getUriFormat())
                .setParameter("metadataPrefix", METADATA_PREFIX);
    }
}
