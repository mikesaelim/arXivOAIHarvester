package io.github.mikesaelim.arxivoaiharvester.model.request;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;

/**
 * A ListRecords request, used to retrieve a range of records between two datestamps.
 *
 * Note that ListRecords responses may be paginated.  This class only represents the initial request to the repository,
 * and subsequent pages are retrieved by sending {@link ResumeListRecordsRequest}s to the harvester, which contain the
 * resumption token sent back by the last response.
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ListRecordsRequest extends ArxivRequest {

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
    private final URI uri;

    /**
     * Constructs a ListRecordsRequest object.  All parameters are optional.
     * @throws IllegalArgumentException if fromDate is after untilDate.
     * @throws URISyntaxException if the input did not create a valid URI
     */
    public ListRecordsRequest(LocalDate fromDate, LocalDate untilDate, String setSpec)
            throws IllegalArgumentException, URISyntaxException {
        super(Verb.LIST_RECORDS);

        this.fromDate = fromDate;
        this.untilDate = untilDate;
        this.setSpec = setSpec;

        if (fromDate != null && untilDate != null && fromDate.isAfter(untilDate)) {
            throw new IllegalArgumentException("tried to create ListRecordsRequest with invalid datestamp range");
        }

        uri = constructURI();
    }

    private URI constructURI() throws URISyntaxException {
        URIBuilder uriBuilder = getUriBuilder()
                .setParameter("metadataPrefix", METADATA_PREFIX);

        if (fromDate != null) {
            uriBuilder.setParameter("from", fromDate.toString());
        }
        if (untilDate != null) {
            uriBuilder.setParameter("until", untilDate.toString());
        }
        if (setSpec != null) {
            uriBuilder.setParameter("setSpec", setSpec);
        }

        return uriBuilder.build();
    }

}