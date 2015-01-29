package mikesaelim.arxivoaiharvester.io;

import org.junit.Test;

import static org.junit.Assert.*;

public class ArxivGetRecordRequestTest {

    @Test(expected = NullPointerException.class)
    public void nullIdentifierShouldThrowException() throws Exception {
        new ArxivGetRecordRequest(null);
    }

    @Test
    public void testGetInitialUri() throws Exception {
        String identifier = "oai:arXiv.org:1302.2146";

        ArxivGetRecordRequest request = new ArxivGetRecordRequest(identifier);

        assertEquals("http://export.arxiv.org/oai2?verb=GetRecord&metadataPrefix=arXivRaw&identifier=oai%3AarXiv.org%3A1302.2146",
                request.getInitialUri().toString());
    }

    @Test
    public void testConstructor_WithoutPrefix() throws Exception {
        String identifier = "1302.2146";

        ArxivGetRecordRequest request = new ArxivGetRecordRequest(identifier);

        assertEquals("oai:arXiv.org:1302.2146", request.getIdentifier());
    }

    @Test
    public void testGetResumptionUri() throws Exception {
        ArxivGetRecordRequest request = new ArxivGetRecordRequest("whatever");

        assertEquals("http://export.arxiv.org/oai2?verb=GetRecord&resumptionToken=something",
                request.getResumptionURI("something").toString());
    }

}