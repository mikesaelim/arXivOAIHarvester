package mikesaelim.arxivoaiharvester;

import mikesaelim.arxivoaiharvester.data.ArticleMetadata;
import mikesaelim.arxivoaiharvester.data.ArticleVersion;
import mikesaelim.arxivoaiharvester.io.ArxivRequest;
import mikesaelim.arxivoaiharvester.io.ArxivResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ArxivOAIHarvesterTest {

    private ArxivOAIHarvester harvester;

    @Mock
    private ArxivRequest request;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        harvester = new ArxivOAIHarvester(request);
    }

    @Test
    public void testParseXMLStream_GetRecords() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("SampleGetRecordResponse.xml");

        ArxivResponse response = harvester.parseXMLStream(inputStream);

        inputStream.close();

        assertSame(request, response.getArxivRequest());
        assertEquals(ZonedDateTime.of(2015, 1, 6, 20, 48, 16, 0, ZoneId.of("Z")), response.getResponseDate());
        assertNull(response.getResumptionToken());
        assertNull(response.getCursor());
        assertNull(response.getCompleteListSize());

        List<ArticleMetadata> records = response.getRecords();
        assertEquals(1, records.size());

        ArticleMetadata record = records.get(0);
        assertEquals(ZonedDateTime.of(2015, 1, 6, 20, 48, 16, 0, ZoneId.of("Z")), record.getRetrievalDateTime());
        assertEquals("oai:arXiv.org:1302.2146", record.getIdentifier());
        assertEquals(LocalDate.of(2013, 4, 3), record.getDatestamp());

        Set<String> sets = record.getSets();
        assertNotNull(sets);
        assertEquals(2, sets.size());
        assertTrue(sets.contains("physics:hep-ex"));
        assertTrue(sets.contains("physics:hep-ph"));

        assertFalse(record.isDeleted());
        assertEquals("1302.2146", record.getId());
        assertEquals("Michael Saelim", record.getSubmitter());

        Set<ArticleVersion> versions = record.getVersions();
        assertEquals(2, versions.size());

        boolean foundV1 = false, foundV2 = false, foundOther = false;
        for (ArticleVersion version : versions) {
            if (version.getVersionNumber() == 1) {
                foundV1 = true;
                assertEquals(ZonedDateTime.of(2013, 2, 8, 21, 0, 1, 0, ZoneId.of("Z")), version.getSubmissionTime());
                assertEquals("853kb", version.getSize());
                assertEquals("D", version.getSourceType());
            } else if (version.getVersionNumber() == 2) {
                foundV2 = true;
                assertEquals(ZonedDateTime.of(2013, 4, 2, 1, 50, 8, 0, ZoneId.of("Z")), version.getSubmissionTime());
                assertEquals("849kb", version.getSize());
                assertEquals("D", version.getSourceType());
            } else {
                foundOther = true;
            }
        }
        assertTrue(foundV1);
        assertTrue(foundV2);
        assertFalse(foundOther);

        assertEquals("The Same-Sign Dilepton Signature of RPV/MFV SUSY", record.getTitle());
        assertEquals("Joshua Berger, Maxim Perelstein, Michael Saelim and Philip Tanedo", record.getAuthors());

        List<String> categories = record.getCategories();
        assertEquals(2, categories.size());
        assertEquals("hep-ph", categories.get(0));
        assertEquals("hep-ex", categories.get(1));

        assertEquals("18 pages, 6 figures; v2: References added", record.getComments());
        assertNull(record.getProxy());
        assertNull(record.getReportNo());
        assertNull(record.getAcmClass());
        assertNull(record.getMscClass());
        assertNull(record.getJournalRef());
        assertNull(record.getDoi());
        assertEquals("http://arxiv.org/licenses/nonexclusive-distrib/1.0/", record.getLicense());
        assertEquals("The lack of observation of superpartners at the Large Hadron Collider so far has led to a renewed interest in supersymmetric models with R-parity violation (RPV). In particular, imposing the Minimal Flavor Violation (MFV) hypothesis on a general RPV model leads to a realistic and predictive framework. Naturalness suggests that stops and gluinos should appear at or below the TeV mass scale. We consider a simplified model with these two particles and MFV couplings. The model predicts a significant rate of events with same-sign dileptons and b-jets. We re-analyze a recent CMS search in this channel and show that the current lower bound on the gluino mass is about 800 GeV at 95% confidence level, with only a weak dependence on the stop mass as long as the gluino can decay to an on-shell top-stop pair. We also discuss how this search can be further optimized for the RPV/MFV scenario, using the fact that MFV stop decays often result in jets with large invariant mass. With the proposed improvements, we estimate that gluino masses of up to about 1.4 TeV can be probed at the 14 TeV LHC with a 100 fb^-1 data set.",
                record.getArticleAbstract());
    }
}