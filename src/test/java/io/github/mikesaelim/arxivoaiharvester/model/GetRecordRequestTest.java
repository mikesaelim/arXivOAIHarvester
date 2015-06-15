package io.github.mikesaelim.arxivoaiharvester.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GetRecordRequestTest {

    @Test(expected = NullPointerException.class)
    public void nullIdentifierShouldThrowException() throws Exception {
        new GetRecordRequest(null);
    }

    @Test
    public void testGetUri() throws Exception {
        String identifier = "oai:arXiv.org:1302.2146";

        GetRecordRequest request = new GetRecordRequest(identifier);

        assertEquals("http://export.arxiv.org/oai2?verb=GetRecord&metadataPrefix=arXivRaw&identifier=oai%3AarXiv.org%3A1302.2146",
                request.getUri().toString());
    }

    @Test
    public void testConstructor_WithoutPrefix() throws Exception {
        String identifier = "1302.2146";

        GetRecordRequest request = new GetRecordRequest(identifier);

        assertEquals("oai:arXiv.org:1302.2146", request.getIdentifier());
        assertEquals("http://export.arxiv.org/oai2?verb=GetRecord&metadataPrefix=arXivRaw&identifier=oai%3AarXiv.org%3A1302.2146",
                request.getUri().toString());
    }

}