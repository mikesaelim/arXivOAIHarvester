package mikesaelim.arxivoaiharvester;

import com.google.common.annotations.VisibleForTesting;
import mikesaelim.arxivoaiharvester.io.ArxivRequest;
import mikesaelim.arxivoaiharvester.io.ArxivResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;

/**
 * TODO: big-ass description, javadoc on methods
 *
 * Created by donerkebab on 1/3/15.
 */
public class ArxivOAIHarvester {

    private final ArxivRequest arxivRequest;
    // TODO: other fields for flow control

    public ArxivOAIHarvester(ArxivRequest arxivRequest) {
        this.arxivRequest = arxivRequest;
    }

    public ArxivResponse getNextBatch() throws IOException, ClientProtocolException, Exception {
        // TODO: the Exception in the throws clause is temporary, only used until we handle response codes and flow control properly
        // TODO: persistent HttpClient instance?  Closeable objects?  HttpClient passed in, leading to unit tests?
        HttpClient httpClient = HttpClientBuilder.create().build();

        HttpGet httpRequest = new HttpGet(arxivRequest.getUri());
        httpRequest.addHeader("User-Agent", arxivRequest.getUserAgentHeader());
        httpRequest.addHeader("From", arxivRequest.getFromHeader());

        HttpResponse httpResponse = httpClient.execute(httpRequest);
        int httpStatusCode = httpResponse.getStatusLine().getStatusCode();

        // TODO: handle other response codes
        if (httpStatusCode != 200) {
            throw new Exception("Returned status code " + httpStatusCode + " !");
        }

        // TODO: implement resumption token storage and other flow control stuff

        return parseXMLStream(httpResponse.getEntity().getContent());
    }

    public boolean hasNextBatch() {
        // TODO: implement
        return false;
    }

    public ArxivRequest getArxivRequest() {
        return arxivRequest;
    }

    @VisibleForTesting ArxivResponse parseXMLStream(InputStream inputStream)
            throws ParserConfigurationException, SAXException, IOException {
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        ArxivResponse.ArxivResponseBuilder responseBuilder = ArxivResponse.builder().arxivRequest(arxivRequest);

        parser.parse(inputStream, new XMLHandler(responseBuilder));

        return responseBuilder.build();
    }

}
