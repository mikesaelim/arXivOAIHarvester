package mikesaelim.arxivoaiharvester.io;

import lombok.Getter;
import lombok.Setter;

import java.net.URI;

/**
 * The complete set of information needed to construct a request to send to the arXiv OAI repository.  There are different
 * implementations of this class for different request verbs.
 *
 * Created by Mike Saelim on 1/3/15.
 */
@Getter
@Setter
public abstract class ArxivRequest {

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
     * Generate the request URI.
     * @return request URI
     */
    public abstract URI getUri();

}
