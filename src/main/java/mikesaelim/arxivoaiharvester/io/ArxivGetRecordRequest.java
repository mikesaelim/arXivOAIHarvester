package mikesaelim.arxivoaiharvester.io;

import lombok.Getter;
import lombok.NonNull;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * An ArxivRequest for the GetRecord verb.  This request is used to retrieve a single record from the repository, by its
 * identifier.
 *
 * Created by Mike Saelim on 1/5/15.
 */
@Getter
public class ArxivGetRecordRequest extends ArxivRequest {

    /**
     * Unique identifier of the record.
     */
    private final String identifier;


    public ArxivGetRecordRequest(@NonNull String identifier) {
        super(Verb.GET_RECORD);
        this.identifier = identifier;
    }

    @Override
    public URI getURI() throws URISyntaxException {
        return new URIBuilder()
                .setScheme("http")
                .setHost(HOST)
                .setPath(PATH)
                .setParameter("verb", this.getVerb().getUriFormat())
                .setParameter("metadataPrefix", METADATA_PREFIX)
                .setParameter("identifier", identifier)
                .build();
    }

}
