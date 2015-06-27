package io.github.mikesaelim.arxivoaiharvester.model.request;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ResumeListRecordsRequestTest {

    @Test(expected = NullPointerException.class)
    public void nullResumptionTokenShouldThrow() throws Exception {
        new ResumeListRecordsRequest(null, new ListRecordsRequest(null, null, null));
    }

    @Test(expected = NullPointerException.class)
    public void nullOriginalRequestShouldThrow() throws Exception {
        new ResumeListRecordsRequest("resumptionToken", null);
    }

    @Test
    public void testGetUri() throws Exception {
        String resumptionToken = "pie";

        ResumeListRecordsRequest request = new ResumeListRecordsRequest(resumptionToken, new ListRecordsRequest(null, null, null));

        assertEquals("http://export.arxiv.org/oai2?verb=ListRecords&resumptionToken=pie", request.getUri().toString());
    }
}