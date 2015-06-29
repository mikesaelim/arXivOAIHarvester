package io.github.mikesaelim.arxivoaiharvester;

import com.google.common.collect.Lists;
import io.github.mikesaelim.arxivoaiharvester.exception.*;
import io.github.mikesaelim.arxivoaiharvester.model.data.ArticleMetadata;
import io.github.mikesaelim.arxivoaiharvester.model.request.GetRecordRequest;
import io.github.mikesaelim.arxivoaiharvester.model.request.ListRecordsRequest;
import io.github.mikesaelim.arxivoaiharvester.model.request.ResumeListRecordsRequest;
import io.github.mikesaelim.arxivoaiharvester.model.response.GetRecordResponse;
import io.github.mikesaelim.arxivoaiharvester.model.response.ListRecordsResponse;
import io.github.mikesaelim.arxivoaiharvester.xml.ParsedXmlResponse;
import io.github.mikesaelim.arxivoaiharvester.xml.XMLParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Stopwatch;
import org.mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ArxivOAIHarvesterTest {

    private ArxivOAIHarvester harvester;


    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private XMLParser xmlParser;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CloseableHttpResponse httpResponse;

    @Captor
    private ArgumentCaptor<HttpGet> getRequestCaptor;

    @Rule
    public Stopwatch stopwatch = new Stopwatch() {};


    // Harvester test settings - we use short wait times for testing
    private int MAX_NUM_RETRIES = 2;
    private Duration MIN_WAIT_BETWEEN_REQUESTS = Duration.ofSeconds(2);
    private Duration MAX_WAIT_BETWEEN_REQUESTS = Duration.ofSeconds(5);
    private String USER_AGENT_HEADER = "Dave's thing that needs a harvester, v0.1";
    private String FROM_HEADER = "dave@daves.com";

    private GetRecordRequest getRecordRequest;
    private ListRecordsRequest listRecordsRequest;
    private ResumeListRecordsRequest resumeListRecordsRequest;
    private ParsedXmlResponse parsedXmlResponse;



    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // Short wait times for testing
        harvester = new ArxivOAIHarvester(httpClient, xmlParser,
                MAX_NUM_RETRIES, MIN_WAIT_BETWEEN_REQUESTS, MAX_WAIT_BETWEEN_REQUESTS);
        harvester.setUserAgentHeader(USER_AGENT_HEADER);
        harvester.setFromHeader(FROM_HEADER);

        getRecordRequest = new GetRecordRequest("oai:arXiv.org:1302.2146");
        listRecordsRequest = new ListRecordsRequest(LocalDate.of(2015, 6, 27), null, "physics:hep-ph");
        resumeListRecordsRequest = new ResumeListRecordsRequest("870434|1001", listRecordsRequest);

        parsedXmlResponse = ParsedXmlResponse.builder()
                .responseDate(ZonedDateTime.now())
                .records(Lists.newArrayList(ArticleMetadata.builder().build()))
                .resumptionToken("resumptionToken")
                .cursor(BigInteger.ONE)
                .completeListSize(BigInteger.TEN)
                .build();
    }



    // Tests: Happy path - 200 OK with successful response

    @Test
    public void testHarvestGetRecord() throws Exception {
        givenHttpResponseIsOK();
        givenRepositoryResponseIsSuccessful();

        GetRecordResponse response = harvester.harvest(getRecordRequest);

        verify(httpClient).execute(getRequestCaptor.capture());
        HttpGet getRequest = getRequestCaptor.getValue();
        assertEquals(getRecordRequest.getUri(), getRequest.getURI());
        assertEquals(USER_AGENT_HEADER, getRequest.getFirstHeader(HttpHeaders.USER_AGENT).getValue());
        assertEquals(FROM_HEADER, getRequest.getFirstHeader(HttpHeaders.FROM).getValue());

        assertEquals(parsedXmlResponse.getResponseDate(), response.getResponseDate());
        assertEquals(getRecordRequest, response.getRequest());
        assertEquals(parsedXmlResponse.getRecords().get(0), response.getRecord());
    }

    @Test
    public void testHarvestListRecords() throws Exception {
        givenHttpResponseIsOK();
        givenRepositoryResponseIsSuccessful();

        ListRecordsResponse response = harvester.harvest(listRecordsRequest);

        verify(httpClient).execute(getRequestCaptor.capture());
        HttpGet getRequest = getRequestCaptor.getValue();
        assertEquals(listRecordsRequest.getUri(), getRequest.getURI());
        assertEquals(USER_AGENT_HEADER, getRequest.getFirstHeader(HttpHeaders.USER_AGENT).getValue());
        assertEquals(FROM_HEADER, getRequest.getFirstHeader(HttpHeaders.FROM).getValue());

        assertEquals(parsedXmlResponse.getResponseDate(), response.getResponseDate());
        assertEquals(listRecordsRequest, response.getRequest());
        assertTrue(parsedXmlResponse.getRecords().containsAll(response.getRecords()));
        assertEquals(parsedXmlResponse.getResumptionToken(), response.getResumptionToken());
        assertEquals(parsedXmlResponse.getCursor(), response.getCursor());
        assertEquals(parsedXmlResponse.getCompleteListSize(), response.getCompleteListSize());
    }

    @Test
    public void testHarvestResumeListRecords() throws Exception {
        givenHttpResponseIsOK();
        givenRepositoryResponseIsSuccessful();

        ListRecordsResponse response = harvester.harvest(resumeListRecordsRequest);

        verify(httpClient).execute(getRequestCaptor.capture());
        HttpGet getRequest = getRequestCaptor.getValue();
        assertEquals(resumeListRecordsRequest.getUri(), getRequest.getURI());
        assertEquals(USER_AGENT_HEADER, getRequest.getFirstHeader(HttpHeaders.USER_AGENT).getValue());
        assertEquals(FROM_HEADER, getRequest.getFirstHeader(HttpHeaders.FROM).getValue());

        assertEquals(parsedXmlResponse.getResponseDate(), response.getResponseDate());
        assertEquals(resumeListRecordsRequest.getOriginalRequest(), response.getRequest());
        assertTrue(parsedXmlResponse.getRecords().containsAll(response.getRecords()));
        assertEquals(parsedXmlResponse.getResumptionToken(), response.getResumptionToken());
        assertEquals(parsedXmlResponse.getCursor(), response.getCursor());
        assertEquals(parsedXmlResponse.getCompleteListSize(), response.getCompleteListSize());
    }



    // Tests: happy path with retries

    @Test
    public void testHarvestWithRetries() throws Exception {
        // The first wait time returned by the repository is between the minimum and maximum.
        CloseableHttpResponse waitHttpResponse1 = mock(CloseableHttpResponse.class,
                withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        when(waitHttpResponse1.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_SERVICE_UNAVAILABLE);
        when(waitHttpResponse1.getFirstHeader(HttpHeaders.RETRY_AFTER).getValue()).thenReturn("3");
        // The second wait time returned by the repository is below the minimum, so it should get bumped up to the minimum.
        CloseableHttpResponse waitHttpResponse2 = mock(CloseableHttpResponse.class,
                withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        when(waitHttpResponse2.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_SERVICE_UNAVAILABLE);
        when(waitHttpResponse2.getFirstHeader(HttpHeaders.RETRY_AFTER).getValue()).thenReturn("1");
        // This now configures the final response from the repository
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getEntity().getContent()).thenReturn(mock(InputStream.class));
        givenRepositoryResponseIsSuccessful();

        when(httpClient.execute(any(HttpGet.class))).thenReturn(waitHttpResponse1)
                                                    .thenReturn(waitHttpResponse2)
                                                    .thenReturn(httpResponse);

        long startTime = stopwatch.runtime(TimeUnit.NANOSECONDS);
        GetRecordResponse response = harvester.harvest(getRecordRequest);
        long endTime = stopwatch.runtime(TimeUnit.NANOSECONDS);

        verify(httpClient, times(3)).execute(any(HttpGet.class));
        verify(xmlParser, times(1)).parse(any(InputStream.class));

        Duration elapsedTime = Duration.ofNanos(endTime - startTime);
        assertTrue(elapsedTime.compareTo(Duration.ofSeconds(5)) > 0);

        assertEquals(parsedXmlResponse.getResponseDate(), response.getResponseDate());
    }



    // Tests: initial wait caused by trying to send a new request too soon after the last one

    @Test
    public void testHarvestWithInitialWait() throws Exception {
        // This makes the harvest finish without parsing
        givenHttpResponseIsNotFound();

        try {
            harvester.harvest(getRecordRequest);
        } catch (RepositoryError e) {
            // do nothing
        }

        long startTime = stopwatch.runtime(TimeUnit.NANOSECONDS);
        try {
            harvester.harvest(getRecordRequest);
        } catch (RepositoryError e) {
            // do nothing
        }
        long endTime = stopwatch.runtime(TimeUnit.NANOSECONDS);

        Duration elapsedTime = Duration.ofNanos(endTime - startTime);
        assertTrue(elapsedTime.compareTo(MIN_WAIT_BETWEEN_REQUESTS) > 0);
    }



    // Tests: timeouts

    @Test(expected = TimeoutException.class)
    public void tooManyRetriesShouldThrow() throws Exception {
        CloseableHttpResponse waitHttpResponse = mock(CloseableHttpResponse.class,
                withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        when(waitHttpResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_SERVICE_UNAVAILABLE);
        when(waitHttpResponse.getFirstHeader(HttpHeaders.RETRY_AFTER).getValue()).thenReturn("2");

        when(httpClient.execute(any(HttpGet.class))).thenReturn(waitHttpResponse);

        harvester.harvest(getRecordRequest);
    }

    @Test(expected = TimeoutException.class)
    public void waitTooLongShouldThrow() throws Exception {
        CloseableHttpResponse waitHttpResponse = mock(CloseableHttpResponse.class,
                withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        when(waitHttpResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_SERVICE_UNAVAILABLE);
        when(waitHttpResponse.getFirstHeader(HttpHeaders.RETRY_AFTER).getValue()).thenReturn("6");

        when(httpClient.execute(any(HttpGet.class))).thenReturn(waitHttpResponse);

        harvester.harvest(getRecordRequest);

    }



    // Tests: problems retrieving response

    @Test(expected = HttpException.class)
    public void problemReachingRepositoryShouldThrow() throws Exception {
        givenErrorReachingRepository();

        harvester.harvest(getRecordRequest);
    }

    @Test(expected = HttpException.class)
    public void problemCreatingInputStreamShouldThrow() throws Exception {
        givenErrorCreatingInputStream();

        harvester.harvest(getRecordRequest);
    }



    // Tests: problems from XML parser

    @Test(expected = ParseException.class)
    public void shouldConveyParseExceptionFromXMLParser() throws Exception {
        givenHttpResponseIsOK();
        givenRepositoryResponseCannotBeParsed();

        harvester.harvest(getRecordRequest);
    }

    @Test(expected = RepositoryError.class)
    public void shouldConveyRepositoryErrorFromXMLParser() throws Exception {
        givenHttpResponseIsOK();
        givenRepositoryResponseIsInvalid();

        harvester.harvest(getRecordRequest);
    }

    @Test(expected = BadArgumentException.class)
    public void shouldConveyBadArgumentExceptionFromXMLParser() throws Exception {
        givenHttpResponseIsOK();
        givenRepositoryResponseIsBadArgument();

        harvester.harvest(getRecordRequest);
    }

    @Test(expected = BadResumptionTokenException.class)
    public void shouldConveyBadResumptionTokenExceptionFromXMLParser() throws Exception {
        givenHttpResponseIsOK();
        givenRepositoryResponseIsBadResumptionToken();

        harvester.harvest(getRecordRequest);
    }



    // Tests: other http response codes

    @Test(expected = UnsupportedRedirectException.class)
    public void receivingRedirectShouldThrow() throws Exception {
        givenHttpResponseIsRedirect();

        harvester.harvest(getRecordRequest);
    }

    @Test(expected = RepositoryError.class)
    public void receivingNotFoundShouldThrow() throws Exception {
        givenHttpResponseIsNotFound();

        harvester.harvest(getRecordRequest);
    }

    @Test(expected = RepositoryError.class)
    public void receivingOtherStatusCodeShouldThrow() throws Exception {
        givenHttpResponseIsOther();

        harvester.harvest(getRecordRequest);
    }





    // Givens

    private void givenHttpResponseIsOK() throws Exception {
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getEntity().getContent()).thenReturn(mock(InputStream.class));
    }

    private void givenHttpResponseIsRedirect() throws Exception {
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_MOVED_TEMPORARILY);
    }

    private void givenHttpResponseIsNotFound() throws Exception {
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
    }

    private void givenHttpResponseIsOther() throws Exception {
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        when(httpResponse.getStatusLine().getReasonPhrase()).thenReturn("Some reason, I dunno");
        when(httpResponse.getEntity()).thenReturn(mock(HttpEntity.class));
    }


    private void givenErrorReachingRepository() throws Exception {
        when(httpClient.execute(any(HttpGet.class))).thenThrow(new IOException());
    }

    private void givenErrorCreatingInputStream() throws Exception {
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getEntity().getContent()).thenThrow(new IllegalStateException());
    }


    private void givenRepositoryResponseIsSuccessful() throws Exception {
        when(xmlParser.parse(any(InputStream.class))).thenReturn(parsedXmlResponse);
    }

    private void givenRepositoryResponseCannotBeParsed() throws Exception {
        when(xmlParser.parse(any(InputStream.class))).thenThrow(new ParseException());
    }

    private void givenRepositoryResponseIsInvalid() throws Exception {
        when(xmlParser.parse(any(InputStream.class))).thenThrow(new RepositoryError());
    }

    private void givenRepositoryResponseIsBadArgument() throws Exception {
        when(xmlParser.parse(any(InputStream.class))).thenThrow(new BadArgumentException());
    }

    private void givenRepositoryResponseIsBadResumptionToken() throws Exception {
        when(xmlParser.parse(any(InputStream.class))).thenThrow(new BadResumptionTokenException());
    }

}