package mikesaelim.arxivoaiharvester.io;

import lombok.Getter;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;

/**
 * An ArxivRequest for the ListRecords verb.  This request is used to retrieve a range of records between two datestamps.
 *
 * Created by Mike Saelim on 1/5/15.
 */
@Getter
public class ArxivListRecordsRequest extends ArxivRequest {

    /**
     * Optional lower bound of the datestamp range.  If null, the range is unbounded from below.
     */
    private final LocalDate fromDate;

    /**
     * Optional upper bound of the datestamp range.  If null, the range is unbounded from above.
     */
    private final LocalDate untilDate;

    /**
     * Optional set to restrict the retrieval to.  If null, the retrieval is not restricted to any set.
     */
    private final String setSpec;

    /**
     * The URI for the initial request to the repository, created from these settings.
     */
    private final URI initialUri;

    /**
     * Constructs an ArxivListRecordsRequest object.  All parameters are optional.
     * @throws IllegalArgumentException if fromDate is after untilDate.
     * @throws URISyntaxException if the input did not create a valid initial URI

     */
    public ArxivListRecordsRequest(LocalDate fromDate, LocalDate untilDate, String setSpec)
            throws IllegalArgumentException, URISyntaxException {
        super(Verb.LIST_RECORDS);
        this.fromDate = fromDate;
        this.untilDate = untilDate;
        this.setSpec = setSpec;

        if (fromDate != null && untilDate != null && fromDate.isAfter(untilDate)) {
            throw new IllegalArgumentException("tried to create ArxivListRecordsRequest with invalid datestamp range");
        }

        initialUri = constructInitialURI();
    }

    private URI constructInitialURI() throws URISyntaxException {
        URIBuilder uriBuilder = getInitialUriBuilder();
        if (fromDate != null) {
            uriBuilder.addParameter("from", fromDate.toString());
        }
        if (untilDate != null) {
            uriBuilder.addParameter("until", untilDate.toString());
        }
        if (setSpec != null) {
            uriBuilder.addParameter("setSpec", setSpec);
        }

        return uriBuilder.build();
    }

}