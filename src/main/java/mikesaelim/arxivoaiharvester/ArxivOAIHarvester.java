package mikesaelim.arxivoaiharvester;

import com.google.common.annotations.VisibleForTesting;
import mikesaelim.arxivoaiharvester.io.ArxivRequest;
import mikesaelim.arxivoaiharvester.io.ArxivResponse;
import org.apache.commons.io.input.ReaderInputStream;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

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

    public ArxivResponse getNextBatch() {
        // TODO: implement
        return null;
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
        // Unfortunately, we must filter out spurious newlines from the corrupted XML response of arXiv's OAI repository.
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
        NewlineFilterReader newlineFilterReader = new NewlineFilterReader(inputStreamReader);
        InputSource filteredInputSource = new InputSource(newlineFilterReader);

        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        ArxivResponse.ArxivResponseBuilder responseBuilder = ArxivResponse.builder().arxivRequest(arxivRequest);

        parser.parse(filteredInputSource, new XMLHandler(responseBuilder));

        return responseBuilder.build();
    }

}
