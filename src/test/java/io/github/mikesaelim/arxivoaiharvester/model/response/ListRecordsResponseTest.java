package io.github.mikesaelim.arxivoaiharvester.model.response;

import io.github.mikesaelim.arxivoaiharvester.model.request.ListRecordsRequest;
import io.github.mikesaelim.arxivoaiharvester.model.request.ResumeListRecordsRequest;
import org.junit.Test;

import static org.junit.Assert.*;

public class ListRecordsResponseTest {

    @Test
    public void testResumption() throws Exception {
        ListRecordsResponse response = ListRecordsResponse.builder()
                .request(new ListRecordsRequest(null, null, null))
                .resumptionToken("resumptionToken")
                .build();

        ResumeListRecordsRequest resumption = response.resumption();

        assertEquals("resumptionToken", resumption.getResumptionToken());
        assertEquals(response.getRequest(), resumption.getOriginalRequest());
    }

    @Test
    public void testNoResumption() throws Exception {
        ListRecordsResponse response = ListRecordsResponse.builder().build();

        ResumeListRecordsRequest resumption = response.resumption();

        assertEquals(ResumeListRecordsRequest.NONE, resumption);
    }
}