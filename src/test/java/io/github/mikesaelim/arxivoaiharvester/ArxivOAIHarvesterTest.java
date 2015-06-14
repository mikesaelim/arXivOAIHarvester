package io.github.mikesaelim.arxivoaiharvester;

import io.github.mikesaelim.arxivoaiharvester.data.ArticleMetadata;
import io.github.mikesaelim.arxivoaiharvester.data.ArticleVersion;
import io.github.mikesaelim.arxivoaiharvester.io.ArxivError;
import io.github.mikesaelim.arxivoaiharvester.io.ArxivRequest;
import org.apache.http.impl.client.CloseableHttpClient;
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
    private CloseableHttpClient httpClient;
    @Mock
    private ArxivRequest request;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        harvester = new ArxivOAIHarvester(httpClient, request);
    }

    @Test
    public void testParseXmlStream_Error() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("SampleErrorResponse.xml");

        ParsedXmlResponse response = harvester.parseXMLStream(inputStream);

        inputStream.close();

        assertEquals(ZonedDateTime.of(2015, 2, 8, 22, 30, 30, 0, ZoneId.of("Z")), response.getResponseDate());
        assertEquals(ArxivError.Type.INTERNAL_ERROR, response.getError().getErrorType());
        assertEquals(0, response.getRecords().size());
        assertNull(response.getResumptionToken());
        assertNull(response.getCursor());
        assertNull(response.getCompleteListSize());
    }

    @Test
    public void testParseXMLStream_GetRecords() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("SampleGetRecordResponse.xml");

        ParsedXmlResponse response = harvester.parseXMLStream(inputStream);

        inputStream.close();

        assertEquals(ZonedDateTime.of(2015, 1, 6, 20, 48, 16, 0, ZoneId.of("Z")), response.getResponseDate());
        assertNull(response.getError());
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

    @Test
    public void testParseXMLStream_ListRecords() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("SampleListRecordsResponse.xml");

        ParsedXmlResponse response = harvester.parseXMLStream(inputStream);

        inputStream.close();

        assertEquals(ZonedDateTime.of(2015, 1, 6, 20, 49, 59, 0, ZoneId.of("Z")), response.getResponseDate());
        assertNull(response.getError());
        assertEquals("726959|1001", response.getResumptionToken());
        assertEquals(0, response.getCursor().intValue());
        assertEquals(58011, response.getCompleteListSize().intValue());

        List<ArticleMetadata> records = response.getRecords();
        assertEquals(6, records.size());

        // Here I'm mostly concerned about whether the titles, authors, and abstracts are retrieved correctly.
        assertEquals("Density dependence of the symmetry energy and the nuclear equation of state: A Dynamical and Statistical model perspective",
                records.get(0).getTitle());
        assertEquals("D.V. Shetty, S.J. Yennello, and G.A. Souliotis", records.get(0).getAuthors());
        assertEquals("Phys.Rev.C76:024606,2007; Erratum-ibid.C76:039902,2007", records.get(0).getJournalRef());
        assertEquals("10.1103/PhysRevC.76.024606 10.1103/PhysRevC.76.039902", records.get(0).getDoi());
        assertEquals("The density dependence of the symmetry energy in the equation of state of isospin asymmetric nuclear matter is of significant importance for studying the structure of systems as diverse as the neutron-rich nuclei and the neutron stars. A number of reactions using the dynamical and the statistical models of multifragmentation, and the experimental isoscaling observable, is studied to extract information on the density dependence of the symmetry energy. It is observed that the dynamical and the statistical model calculations give consistent results assuming the sequential decay effect in dynamical model to be small. A comparison with several other independent studies is also made to obtain important constraint on the form of the density dependence of the symmetry energy. The comparison rules out an extremely \" stiff \" and \" soft \" form of the density dependence of the symmetry energy with important implications for astrophysical and nuclear physics studies.",
                records.get(0).getArticleAbstract());

        assertEquals("05A15, 05A16", records.get(1).getMscClass());
        assertEquals("We discuss the asymptotic behaviour of models of lattice polygons, mainly on the square lattice. In particular, we focus on limiting area laws in the uniform perimeter ensemble where, for fixed perimeter, each polygon of a given area occurs with the same probability. We relate limit distributions to the scaling behaviour of the associated perimeter and area generating functions, thereby providing a geometric interpretation of scaling functions. To a major extent, this article is a pedagogic review of known results.",
                records.get(1).getArticleAbstract());

        assertEquals("For a smooth projective curve, the cycles of e-secant k-planes are among the most studied objects in classical enumerative geometry and there are well-known formulas due to Castelnuovo, Cayley and MacDonald concerning them. Despite various attempts, surprisingly little is known about the enumerative validity of such formulas. The aim of this paper is to completely clarify this problem in the case of the generic curve C of given genus. Using degeneration techniques and a few facts about the birational geometry of moduli spaces of stable pointed curves we determine precisely under which conditions the cycle of e-secant k-planes in non-empty and we compute its dimension. We also precisely determine the dimension of the variety of linear series on C carrying e-secant k-planes. In a different direction, in the last part of the paper we study the distribution of ramification points of the powers of a line bundle on C having prescribed ramification at a given point.",
                records.get(2).getArticleAbstract());

        assertEquals("The existence and stability under linear perturbation of closed timelike curves in the spacetime associated to Schwarzschild black hole pierced by a spinning string are studied. Due to the superposition of the black hole, we find that the spinning string spacetime is deformed in such a way to allow the existence of closed timelike geodesics.",
                records.get(3).getArticleAbstract());

        assertEquals("Water absorption is identified in the atmosphere of HD209458b by comparing models for the planet's transmitted spectrum to recent, multi-wavelength, eclipse-depth measurements (from 0.3 to 1 microns) published by Knutson et al. (2007). A cloud-free model which includes solar abundances, rainout of condensates, and photoionization of sodium and potassium is in good agreement with the entire set of eclipse-depth measurements from the ultraviolet to near-infrared. Constraints are placed on condensate removal by gravitational settling, the bulk metallicity, and the redistribution of absorbed stellar flux. Comparisons are also made to the Charbonneau et al. (2002) sodium measurements.",
                records.get(4).getArticleAbstract());

        assertEquals("The elastic scattering and breakup of $^{11}$Be from a proton target at intermediate energies is studied. We explore the role of core excitation in the reaction mechanism. Comparison with the data suggests that there is still missing physics in the description.",
                records.get(5).getArticleAbstract());
    }



}