package io.github.mikesaelim.arxivoaiharvester.model.request;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class ListRecordsRequestTest {

    @Test
    public void testGetUri_FullySpecified() throws Exception {
        LocalDate fromDate = LocalDate.of(2014, 11, 25);
        LocalDate untilDate = LocalDate.of(2015, 1, 5);
        String setSpec = "physics";

        ListRecordsRequest request = new ListRecordsRequest(fromDate, untilDate, setSpec);

        assertEquals("http://export.arxiv.org/oai2?verb=ListRecords&metadataPrefix=arXivRaw&from=2014-11-25&until=2015-01-05&set=physics",
                request.getUri().toString());
    }

    @Test
    public void testGetUri_PartiallySpecified() throws Exception {
        LocalDate fromDate = LocalDate.of(2014, 11, 25);
        String setSpec = "physics";

        ListRecordsRequest request = new ListRecordsRequest(fromDate, null, setSpec);

        assertEquals("http://export.arxiv.org/oai2?verb=ListRecords&metadataPrefix=arXivRaw&from=2014-11-25&set=physics",
                request.getUri().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidDatestampRangeShouldThrowException() throws Exception {
        LocalDate fromDate = LocalDate.of(2014, 11, 25);
        LocalDate untilDate = LocalDate.of(2014, 11, 24);

        new ListRecordsRequest(fromDate, untilDate, null);
    }

}