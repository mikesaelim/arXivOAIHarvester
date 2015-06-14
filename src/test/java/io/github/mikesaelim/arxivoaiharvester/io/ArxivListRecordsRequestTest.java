package io.github.mikesaelim.arxivoaiharvester.io;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.*;

public class ArxivListRecordsRequestTest {

    @Test
    public void testGetUri_Full() throws Exception {
        LocalDate fromDate = LocalDate.of(2014, 11, 25);
        LocalDate untilDate = LocalDate.of(2015, 1, 5);
        String setSpec = "physics";

        ArxivListRecordsRequest request = new ArxivListRecordsRequest(fromDate, untilDate, setSpec);

        assertEquals("http://export.arxiv.org/oai2?verb=ListRecords&metadataPrefix=arXivRaw&from=2014-11-25&until=2015-01-05&setSpec=physics",
                request.getInitialUri().toString());
    }

    @Test
    public void testGetUri_Partial() throws Exception {
        LocalDate fromDate = LocalDate.of(2014, 11, 25);
        String setSpec = "physics";

        ArxivListRecordsRequest request = new ArxivListRecordsRequest(fromDate, null, setSpec);

        assertEquals("http://export.arxiv.org/oai2?verb=ListRecords&metadataPrefix=arXivRaw&from=2014-11-25&setSpec=physics",
                request.getInitialUri().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidDatestampRangeShouldThrowException() throws Exception {
        LocalDate fromDate = LocalDate.of(2014, 11, 25);
        LocalDate untilDate = LocalDate.of(2014, 11, 24);

        new ArxivListRecordsRequest(fromDate, untilDate, null);
    }

    @Test
    public void testGetResumptionUri() throws Exception {
        ArxivListRecordsRequest request = new ArxivListRecordsRequest(null, null, "whatever");

        assertEquals("http://export.arxiv.org/oai2?verb=ListRecords&resumptionToken=something",
                request.getResumptionURI("something").toString());
    }

}