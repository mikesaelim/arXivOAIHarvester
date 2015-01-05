package mikesaelim.arxivoaiharvester;

import mikesaelim.arxivoaiharvester.io.ArxivRequest;
import mikesaelim.arxivoaiharvester.io.ArxivResponse;

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

}
