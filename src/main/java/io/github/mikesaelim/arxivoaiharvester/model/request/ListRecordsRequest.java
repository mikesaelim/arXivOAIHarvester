package io.github.mikesaelim.arxivoaiharvester.model.request;

import io.github.mikesaelim.arxivoaiharvester.model.response.ListRecordsResponse;
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
 * Note that ListRecords responses may be paginated.  {@link ResumeListRecordsRequest} is a ListRecordsRequest that
 * continues the original request by sending back the resumption token received from the last response.
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
    protected URI uri;

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
            uriBuilder.setParameter("set", setSpec);
        }

        return uriBuilder.build();
    }



    /**
     * Static dummy value that gets returned when a {@link ListRecordsResponse} has no resumption.
     */
    public static ListRecordsRequest NONE = createNone();

    private static ListRecordsRequest createNone() {
        try {
            return new ListRecordsRequest(LocalDate.MAX, LocalDate.MAX, null);
        } catch (URISyntaxException e) {
            throw new Error("Error creating ListRecordsRequest.NONE");
        }
    }

}