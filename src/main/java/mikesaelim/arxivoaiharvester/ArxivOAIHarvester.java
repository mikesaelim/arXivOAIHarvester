package mikesaelim.arxivoaiharvester;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import mikesaelim.arxivoaiharvester.io.ArxivRequest;
import mikesaelim.arxivoaiharvester.io.ArxivResponse;
import org.apache.http.client.ClientProtocolException;
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

/**
 * TODO: big-ass description, javadoc on methods
 *
 * Created by Mike Saelim on 1/3/15.
 */
public class ArxivOAIHarvester {

    private final Logger log = LoggerFactory.getLogger(ArxivOAIHarvester.class);

    private final SAXParser parser;
    private final CloseableHttpClient httpClient;

    private final ArxivRequest arxivRequest;

    private String resumptionToken;
    private Integer currentPosition = 0;  // TODO: javadoc - number of records already returned = index of next record to be returned since start from 0
    private Integer completeSize;         // TODO: javadoc - complete size of the list
    // TODO: other fields for flow control

    public ArxivOAIHarvester(CloseableHttpClient httpClient, ArxivRequest arxivRequest)
            throws ParserConfigurationException, SAXException{
        parser = SAXParserFactory.newInstance().newSAXParser();
        this.httpClient = httpClient;

        this.arxivRequest = arxivRequest;
    }

    // This takes some time; it's implemented synchronously, and it's up to the user to write an asynchronous call if he/she wants one.
    public ArxivResponse getNextBatch() throws IOException, ClientProtocolException, Exception {
        // TODO: the Exception in the throws clause is temporary, only used until we handle response codes and flow control properly

        log.info("Sending request to arXiv OAI repository: {}", arxivRequest.getUri());

        HttpGet httpRequest = new HttpGet(arxivRequest.getUri());
        httpRequest.addHeader("User-Agent", arxivRequest.getUserAgentHeader());
        httpRequest.addHeader("From", arxivRequest.getFromHeader());

        ParsedXmlResponse parsedXmlResponse;
        try (CloseableHttpResponse httpResponse = httpClient.execute(httpRequest)) {
            int httpStatusCode = httpResponse.getStatusLine().getStatusCode();

            // TODO: handle other response codes
            if (httpStatusCode != 200) {
                throw new Exception("Returned status code " + httpStatusCode + ": " +
                        httpResponse.getStatusLine().getReasonPhrase() + ": " +
                        EntityUtils.toString(httpResponse.getEntity()));
            }

            // TODO: implement resumption token storage and other flow control stuff
            // TODO: handle parsing SAXExceptions?
            log.info("Parsing response from arXiv OAI repository for request {}", arxivRequest.getUri());

            parsedXmlResponse = parseXMLStream(httpResponse.getEntity().getContent());

            log.info("Response parsed for request {}", arxivRequest.getUri());
        }

        ArxivResponse arxivResponse = ArxivResponse.builder()
                .arxivRequest(arxivRequest)
                .responseDate(parsedXmlResponse.getResponseDate())
                .records(ImmutableList.copyOf(parsedXmlResponse.getRecords()))
                .build();

        resumptionToken = parsedXmlResponse.getResumptionToken();
        // TODO: ensure consistency with cursor?
        currentPosition += parsedXmlResponse.getRecords().size();
        // TODO: perhaps only set this on first response
        completeSize = parsedXmlResponse.getCompleteListSize() != null ?
                parsedXmlResponse.getCompleteListSize() : parsedXmlResponse.getRecords().size();

        return arxivResponse;
    }

    public boolean hasNextBatch() {
        // TODO: implement
        return false;
    }

    public ArxivRequest getArxivRequest() {
        return arxivRequest;
    }

    public Integer getCurrentPosition() {
        return currentPosition;
    }

    // Null if not known/valid
    public Integer getCompleteSize() {
        return completeSize;
    }

    @VisibleForTesting ParsedXmlResponse parseXMLStream(InputStream inputStream) throws SAXException, IOException {
        ParsedXmlResponse parsedXmlResponse = new ParsedXmlResponse();
        parser.parse(inputStream, new XMLHandler(parsedXmlResponse));
        return parsedXmlResponse;
    }

}
