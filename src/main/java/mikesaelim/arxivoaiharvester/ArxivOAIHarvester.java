package mikesaelim.arxivoaiharvester;

import com.google.common.annotations.VisibleForTesting;
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
    // TODO: other fields for flow control

    public ArxivOAIHarvester(CloseableHttpClient httpClient, ArxivRequest arxivRequest)
            throws ParserConfigurationException, SAXException{
        parser = SAXParserFactory.newInstance().newSAXParser();
        this.httpClient = httpClient;

        this.arxivRequest = arxivRequest;
    }

    public ArxivResponse getNextBatch() throws IOException, ClientProtocolException, Exception {
        // TODO: the Exception in the throws clause is temporary, only used until we handle response codes and flow control properly
        ArxivResponse arxivResponse;

        log.info("Sending request to arXiv OAI repository: {}", arxivRequest.getUri());

        HttpGet httpRequest = new HttpGet(arxivRequest.getUri());
        httpRequest.addHeader("User-Agent", arxivRequest.getUserAgentHeader());
        httpRequest.addHeader("From", arxivRequest.getFromHeader());

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

            arxivResponse = parseXMLStream(httpResponse.getEntity().getContent());

            log.info("Response parsed for request {}", arxivRequest.getUri());
        }

        return arxivResponse;
    }

    public boolean hasNextBatch() {
        // TODO: implement
        return false;
    }

    public ArxivRequest getArxivRequest() {
        return arxivRequest;
    }

    @VisibleForTesting ArxivResponse parseXMLStream(InputStream inputStream) throws SAXException, IOException {
        ArxivResponse.ArxivResponseBuilder responseBuilder = ArxivResponse.builder().arxivRequest(arxivRequest);

        parser.parse(inputStream, new XMLHandler(responseBuilder));

        return responseBuilder.build();
    }

}
