package io.github.mikesaelim.arxivoaiharvester;

import com.google.common.collect.ImmutableList;
import io.github.mikesaelim.arxivoaiharvester.exception.*;
import io.github.mikesaelim.arxivoaiharvester.model.request.GetRecordRequest;
import io.github.mikesaelim.arxivoaiharvester.model.request.ListRecordsRequest;
import io.github.mikesaelim.arxivoaiharvester.model.request.ResumeListRecordsRequest;
import io.github.mikesaelim.arxivoaiharvester.model.response.GetRecordResponse;
import io.github.mikesaelim.arxivoaiharvester.model.response.ListRecordsResponse;
import io.github.mikesaelim.arxivoaiharvester.xml.ParsedXmlResponse;
import io.github.mikesaelim.arxivoaiharvester.xml.XMLParser;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

/**
 * TODO javadoc
 *
 * This class is most definitely not thread-safe.
 */
@Slf4j
public class ArxivOAIHarvester {

    private final CloseableHttpClient httpClient;
    private final XMLParser xmlParser;

    @Getter private final int maxNumRetries;
    @Getter private final Duration minWaitBetweenRequests;
    @Getter private final Duration maxWaitBetweenRequests;

    @Getter @Setter private String userAgentHeader;
    @Getter @Setter private String fromHeader;

    /**
     * Construct a harvester with the default settings:
     * <ul>
     *     <li>Maximum number of retries: 3</li>
     *     <li>Minimum wait time between requests: 10 seconds</li>
     *     <li>Maximum wait time between requests: 5 minutes</li>
     * </ul>
     */
    public ArxivOAIHarvester(CloseableHttpClient httpClient) {
        this(httpClient, 3, Duration.ofSeconds(10), Duration.ofMinutes(5));
    }

    public ArxivOAIHarvester(@NonNull CloseableHttpClient httpClient,
                             int maxNumRetries,
                             @NonNull Duration minWaitBetweenRequests,
                             @NonNull Duration maxWaitBetweenRequests) {
        if (maxNumRetries < 0) {
            throw new IllegalArgumentException("Maximum number of retries must be 0 or greater");
        }
        if (minWaitBetweenRequests.compareTo(maxWaitBetweenRequests) > 0) {
            throw new IllegalArgumentException("Minimum wait time between requests must be less than or equal to maximum");
        }

        this.httpClient = httpClient;
        this.maxNumRetries = maxNumRetries;
        this.minWaitBetweenRequests = minWaitBetweenRequests;
        this.maxWaitBetweenRequests = maxWaitBetweenRequests;

        this.xmlParser = new XMLParser();
    }

    /**
     * See {@link #harvest(URI)} for exceptions.  Not thread-safe.
     */
    public GetRecordResponse harvest(@NonNull GetRecordRequest request) throws InterruptedException {
        ParsedXmlResponse xmlResponse = harvest(request.getUri());

        return GetRecordResponse.builder()
                .responseDate(xmlResponse.getResponseDate())
                .request(request)
                .record(xmlResponse.getRecords().get(0))
                .build();
    }

    /**
     * See {@link #harvest(URI)} for exceptions.  Not thread-safe.
     */
    public ListRecordsResponse harvest(@NonNull ListRecordsRequest request) throws InterruptedException {
        ParsedXmlResponse xmlResponse = harvest(request.getUri());

        return ListRecordsResponse.builder()
                .responseDate(xmlResponse.getResponseDate())
                .request(request)
                .records(ImmutableList.copyOf(xmlResponse.getRecords()))
                .resumptionToken(xmlResponse.getResumptionToken())
                .cursor(xmlResponse.getCursor())
                .completeListSize(xmlResponse.getCompleteListSize())
                .build();
    }

    /**
     * See {@link #harvest(URI)} for exceptions.  Not thread-safe.
     */
    public ListRecordsResponse harvest(@NonNull ResumeListRecordsRequest request) throws InterruptedException {
        ParsedXmlResponse xmlResponse = harvest(request.getUri());

        return ListRecordsResponse.builder()
                .responseDate(xmlResponse.getResponseDate())
                .request(request.getOriginalRequest())
                .records(ImmutableList.copyOf(xmlResponse.getRecords()))
                .resumptionToken(xmlResponse.getResumptionToken())
                .cursor(xmlResponse.getCursor())
                .completeListSize(xmlResponse.getCompleteListSize())
                .build();
    }

    /**
     * Send the harvesting request and retrieve a response.  This method will try multiple times, waiting the appropriate
     * amount of time before retrying.  It is definitely not thread-safe.
     *
     * @param requestUri URI to be sent to the repository
     * @return parsed content of the response from the repository
     *
     * @throws NullPointerException if requestUri is null
     * @throws HttpException if there is a problem communicating with the repository
     * @throws InterruptedException if the process is interrupted
     * @throws TimeoutException if there have been too many retries, or the repository has suggested a wait time that is too long
     * @throws ParseException if parsing fails
     * @throws RepositoryError if the repository's response was parseable but invalid
     * @throws BadArgumentException if the repository's response contains a BadArgument error
     * @throws BadResumptionTokenException if the repository's response contains a BadResumptionToken error
     */
    private ParsedXmlResponse harvest(@NonNull URI requestUri) throws InterruptedException {
        HttpGet httpRequest = new HttpGet(requestUri);
        if (userAgentHeader != null) {
            httpRequest.addHeader("User-Agent", userAgentHeader);
        }
        if (fromHeader != null) {
            httpRequest.addHeader("From", fromHeader);
        }

        int numRetries = 0;
        while (numRetries <= maxNumRetries) {
            RepositoryResponse response = tryHarvest(httpRequest);
            if (response.getParsedXmlResponse() != null) {
                return response.getParsedXmlResponse();
            }

            Duration wait = response.getWait();
            if (wait.compareTo(maxWaitBetweenRequests) > 0) {
                String errorString = "Repository-suggested wait time of " + wait +
                        "exceeds maximum allowed wait time of " + maxWaitBetweenRequests +
                        "; aborting request " + requestUri;
                log.warn(errorString);
                throw new TimeoutException(errorString);
            } else if (wait.compareTo(minWaitBetweenRequests) < 0) {
                wait = minWaitBetweenRequests;
            } else {
                // Padding to help ensure that we don't retry too early and run afoul of the repository's throttling logic
                wait = wait.plusSeconds(5);
            }

            Thread.sleep(wait.toMillis());

            numRetries++;
        }

        String errorString = "Too many retries; aborting request " + requestUri;
        log.warn(errorString);
        throw new TimeoutException(errorString);
    }


    /**
     * Send the harvesting request to the arXiv OAI repository and receiving a response, once.
     *
     * It returns one of three things:
     * <ul>
     *     <li>if the response is 200 OK, the parsed XML data,</li>
     *     <li>if the response is 503 Retry After, the number of seconds that the repository suggests waiting, or</li>
     *     <li>a runtime exception if there is a problem.</li>
     * </ul>
     *
     * The list of runtime exceptions that can be thrown is basically covered in {@link #harvest(URI)}.
     */
    private RepositoryResponse tryHarvest(HttpGet httpRequest) {
        log.info("Sending request to arXiv OAI repository: {}", httpRequest.getURI());

        try (CloseableHttpResponse httpResponse = httpClient.execute(httpRequest)) {
            int httpStatusCode = httpResponse.getStatusLine().getStatusCode();

            switch (httpStatusCode) {
                case HttpStatus.SC_OK:
                    log.info("Parsing response from arXiv OAI repository for request {}", httpRequest.getURI());

                    ParsedXmlResponse parsedXmlResponse;
                    try {
                        parsedXmlResponse = xmlParser.parse(httpResponse.getEntity().getContent());
                    } catch (BadArgumentException | BadResumptionTokenException e) {
                        log.error("Repository complained about input for request " + httpRequest.getURI(), e);
                        throw e;
                    } catch (ParseException | RepositoryError e) {
                        log.error("Error parsing response for request " + httpRequest.getURI(), e);
                        throw e;
                    }

                    log.info("Response parsed for request {}", httpRequest.getURI());

                    return new RepositoryResponse(parsedXmlResponse, null);

                case HttpStatus.SC_MOVED_TEMPORARILY:
                    // Handling this is not currently supported
                    String movedErrorString = "Redirect received for request " + httpRequest.getURI();
                    log.error(movedErrorString);
                    throw new UnsupportedRedirectException(movedErrorString);

                case HttpStatus.SC_NOT_FOUND:
                    String notFoundErrorString = "Received 404 for request " + httpRequest.getURI();
                    log.error(notFoundErrorString);
                    throw new RepositoryError(notFoundErrorString);

                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    Long secondsToWait = Long.parseLong(httpResponse.getFirstHeader(HttpHeaders.RETRY_AFTER).getValue());
                    log.info("Received 503 Retry After; told to wait " + secondsToWait + " seconds");
                    return new RepositoryResponse(null, Duration.ofSeconds(secondsToWait));

                default:
                    // Unfortunately, we currently aren't prepared to handle other HTTP status codes.  The OAI specs
                    // don't really say what to do for most of them.  So we log a warning and return an error response.
                    String defaultErrorString = "Request to arXiv OAI repository " + httpRequest.getURI() +
                            " returned status code " + httpStatusCode + ": " +
                            httpResponse.getStatusLine().getReasonPhrase() + ": " +
                            EntityUtils.toString(httpResponse.getEntity());
                    log.error(defaultErrorString);
                    throw new RepositoryError(defaultErrorString);
            }
        } catch (IOException | IllegalStateException e) {
            log.error("Error retrieving response from arXiv OAI repository for request " + httpRequest.getURI(), e);
            throw new HttpException(e);
        }

    }


    /**
     * POJO to hold responses from the arXiv OAI repository that we can do something with: either a parsed XML response
     * or a number of seconds to wait.  Immutable.
     */
    @Value
    private static class RepositoryResponse {
        ParsedXmlResponse parsedXmlResponse;
        Duration wait;
    }

}
