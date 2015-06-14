package io.github.mikesaelim.arxivoaiharvester;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.github.mikesaelim.arxivoaiharvester.exception.ArxivError;
import io.github.mikesaelim.arxivoaiharvester.xml.ParsedXmlResponse;
import io.github.mikesaelim.arxivoaiharvester.xml.XMLHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * TODO: big-ass description, javadoc on methods; methods are NOT thread-safe; harvester lifecycle and death
 */
public class ArxivOAIHarvester {

    private final Logger log = LoggerFactory.getLogger(ArxivOAIHarvester.class);

    private final SAXParser parser;
    private final CloseableHttpClient httpClient;

    private final ArxivRequest arxivRequest;

    private boolean alive;

    private String nextResumptionToken;
    private Integer recordsReturned;
    private Integer batchesReturned;
    private Integer completeListSize;
    // TODO: other fields for flow control

    public ArxivOAIHarvester(CloseableHttpClient httpClient, ArxivRequest arxivRequest)
            throws ParserConfigurationException, SAXException {
        parser = SAXParserFactory.newInstance().newSAXParser();
        this.httpClient = httpClient;
        this.arxivRequest = arxivRequest;

        alive = true;
        recordsReturned = 0;
        batchesReturned = 0;
    }

    // This takes some time; it's implemented synchronously, and it's up to the user to write an asynchronous call if he/she wants one.
    // Possible IOException, HttpResponseException, or returns an ArxivResponse with an error description.
    public ArxivResponse getNextBatch() throws IOException {
        if (!alive) {
            return handleError(false, ArxivError.Type.DEAD_HARVESTER,
                    "Attempt to retrieve the next batch from a dead harvester.");
        }

        URI nextRequestUri;
        if (batchesReturned == 0) {
            nextRequestUri = arxivRequest.getInitialUri();
        } else {
            if (StringUtils.isEmpty(nextResumptionToken)) {
                return handleError(true, ArxivError.Type.INTERNAL_ERROR,
                        "No resumption token found");
            } else {
                try {
                    nextRequestUri = arxivRequest.getResumptionURI(nextResumptionToken);
                } catch (URISyntaxException e) {
                    return handleError(true, ArxivError.Type.INTERNAL_ERROR,
                            "Resumption token '" + nextResumptionToken + "' created an invalid URI");
                }
            }
        }

        HttpGet httpRequest = new HttpGet(nextRequestUri);
        httpRequest.addHeader("User-Agent", arxivRequest.getUserAgentHeader());
        httpRequest.addHeader("From", arxivRequest.getFromHeader());





        // TODO: Begin loop for handling 503 here?

        log.info("Sending request to arXiv OAI repository: {}", nextRequestUri);

        ParsedXmlResponse parsedXmlResponse;
        try (CloseableHttpResponse httpResponse = httpClient.execute(httpRequest)) {
            int httpStatusCode = httpResponse.getStatusLine().getStatusCode();

            // TODO: handle other response codes?
            switch (httpStatusCode) {
                case HttpStatus.SC_OK:
                    // TODO: implement resumption token storage and other flow control stuff
                    log.info("Parsing response from arXiv OAI repository for request {}", nextRequestUri);

                    try {
                        parsedXmlResponse = parseXMLStream(httpResponse.getEntity().getContent());
                    } catch (SAXException e) {
                        return handleError(false, ArxivError.Type.PARSE_ERROR, e.toString());
                    }

                    log.info("Response parsed for request {}", nextRequestUri);

                    break;
//                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    // TODO: Handle 503
//                    return null;
                default:
                    // Unfortunately, we currently aren't prepared to handle other HTTP status codes.  The OAI specs
                    // don't really say what to do for most of them.  So we log a warning, return an error response,
                    // and permanently kill the harvester.
                    return handleError(false, ArxivError.Type.HTTP_ERROR,
                            new StringBuilder("Returned status code ")
                                .append(httpStatusCode).append(": ")
                                .append(httpResponse.getStatusLine().getReasonPhrase()).append(": ")
                                .append(EntityUtils.toString(httpResponse.getEntity()))
                                .toString());
            }
        }

        if (parsedXmlResponse.getError() != null) {
            return handleError(true, parsedXmlResponse.getError().getErrorType(),
                    parsedXmlResponse.getError().getErrorMessage());
        }

        nextResumptionToken = parsedXmlResponse.getResumptionToken();
        recordsReturned += parsedXmlResponse.getRecords().size();
        batchesReturned++;
        if (batchesReturned == 1) {
            completeListSize = parsedXmlResponse.getCompleteListSize() != null ?
                    parsedXmlResponse.getCompleteListSize() : parsedXmlResponse.getRecords().size();
        }

        return ArxivResponse.builder()
                .arxivRequest(arxivRequest)
                .responseDate(parsedXmlResponse.getResponseDate())
                .records(ImmutableList.copyOf(parsedXmlResponse.getRecords()))
                .build();




    }

    // TODO: javadoc
    public boolean isAlive() {
        return alive;
    }

    public ArxivRequest getArxivRequest() {
        return arxivRequest;
    }

    // TODO: javadoc - number of records already returned = index of next record to be returned since start from 0
    public Integer getRecordsReturned() {
        return recordsReturned;
    }

    // TODO: javadoc - number of batches successfully returned
    public Integer getBatchesReturned() {
        return batchesReturned;
    }

    // TODO: javadoc - complete size of the list
    // Null if not known/valid
    public Integer getCompleteListSize() {
        return completeListSize;
    }

    @VisibleForTesting ParsedXmlResponse parseXMLStream(InputStream inputStream) throws SAXException, IOException {
        ParsedXmlResponse parsedXmlResponse = new ParsedXmlResponse();
        parser.parse(inputStream, new XMLHandler(parsedXmlResponse));
        return parsedXmlResponse;
    }

    /**
     * Handle an error encountered when trying to retrieve the next batch of records.  Kills the harvester permanently.
     * Also logs an error or a warning.
     * @param shouldLogAsError true if this is log level ERROR, false if this is log level WARN
     * @param errorType type of ArxivError
     * @param message message to log and enclose with the ArxivError
     * @return an ArxivResponse with the corresponding error, to be passed back by the harvester
     */
    private ArxivResponse handleError(boolean shouldLogAsError, ArxivError.Type errorType, String message) {
        alive = false;

        if (shouldLogAsError) {
            log.error(message);
        } else {
            log.warn(message);
        }

        return ArxivResponse.builder()
                .arxivRequest(arxivRequest)
                .error(new ArxivError(errorType, message))
                .build();
    }

}
