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

        ListRecordsRequest resumption = response.resumption();

        assertNotEquals(ListRecordsRequest.NONE, resumption);
        assertTrue(resumption instanceof ResumeListRecordsRequest);

        assertEquals("resumptionToken", ((ResumeListRecordsRequest) resumption).getResumptionToken());
        assertEquals(response.getRequest(), ((ResumeListRecordsRequest) resumption).getOriginalRequest());
    }

    @Test
    public void testNoResumption() throws Exception {
        ListRecordsResponse response = ListRecordsResponse.builder().build();

        ListRecordsRequest resumption = response.resumption();

        assertEquals(ListRecordsRequest.NONE, resumption);
    }

    @Test
    public void testBlankResumption() throws Exception {
        ListRecordsResponse response = ListRecordsResponse.builder()
                .resumptionToken("")
                .build();

        ListRecordsRequest resumption = response.resumption();

        assertEquals(ListRecordsRequest.NONE, resumption);
    }

}