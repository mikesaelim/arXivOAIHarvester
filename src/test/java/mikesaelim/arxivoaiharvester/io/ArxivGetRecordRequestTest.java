package mikesaelim.arxivoaiharvester.io;

import org.junit.Test;

import static org.junit.Assert.*;

public class ArxivGetRecordRequestTest {

    @Test(expected = NullPointerException.class)
    public void nullIdentifierShouldThrowException() throws Exception {
        new ArxivGetRecordRequest(null);
    }

    @Test
    public void testGetURI() throws Exception {
        String identifier = "oai:arXiv.org:1302.2146";

        ArxivGetRecordRequest request = new ArxivGetRecordRequest(identifier);

        assertEquals("http://export.arxiv.org/oai2?verb=GetRecord&metadataPrefix=arXivRaw&identifier=oai%3AarXiv.org%3A1302.2146",
                request.getURI().toString());
    }

}