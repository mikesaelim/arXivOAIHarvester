package io.github.mikesaelim.arxivoaiharvester;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.github.mikesaelim.arxivoaiharvester.exception.*;
import io.github.mikesaelim.arxivoaiharvester.model.data.ArticleMetadata;
import io.github.mikesaelim.arxivoaiharvester.model.request.ArxivRequest;
import io.github.mikesaelim.arxivoaiharvester.model.request.GetRecordRequest;
import io.github.mikesaelim.arxivoaiharvester.model.request.ListRecordsRequest;
import io.github.mikesaelim.arxivoaiharvester.model.request.ResumeListRecordsRequest;
import io.github.mikesaelim.arxivoaiharvester.model.response.ArxivResponse;
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
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;

/**
 * This is the central harvester class.
 *
 * Users pass objects extending {@link ArxivRequest} in to one of the public harvest() methods, wait for the harvester
 * to retrieve and parse the results from the arXiv OAI repository, and receive an {@link ArxivResponse} containing the
 * records returned.  This wait generally takes at least seconds, and possibly as much as minutes, as the arXiv OAI
 * repository throttles requests and forces the harvester to retry at a later time.  If any problems are encountered,
 * the harvester throws one of many exceptions detailed in the javadoc for {@link #harvest(URI)}.
 *
 * See the README.md for general information, especially information on using the harvester.
 *
 * This class is most definitely blocking and not thread-safe.  Do not use let more than one thread use it.
 *
 *
 *
 *** Implementation and Design Details
 *
 * Since the harvester effectively functions as a wrapper around a {@link HttpClient}, its interface was slightly based
 * on HttpClient's, receiving requests and returning responses.  Once its initial parameters are set, the harvester is
 * stateless, except for holding the instant that the last response was received for flow control purposes.
 *
 * There are three flow control parameters: the maximum number of retries, the minimum wait between requests to the
 * repository, and the maximum wait.  These are necessary because the repository may send back a 503 Retry-After
 * response that forces the harvester to wait a certain number of seconds before trying again - if the harvester does
 * not comply, the wait increases.
 *
 * So, this harvester complies.  When the user invokes one of the harvest methods with an {@link ArxivRequest}, the
 * harvester first checks the duration since the last response was received, and waits if that duration is smaller than
 * the minimum wait between requests.  If the harvester receives a 503 Retry-After response, the wait duration that the
 * repository suggests could fall in one of three buckets:
 * <ul>
 *     <li>if the suggested wait is less than the minimum wait between requests, the harvester will wait the minimum,</li>
 *     <li>if the suggested wait is between the minimum and maximum, the harvester will pad the wait by a small amount,
 *     because the cost of sending just a little too early is waiting even longer, and</li>
 *     <li>if the suggested wait is more than the maximum wait between requests, the process will timeout.</li>
 * </ul>
 * The process will also timeout if it ends up going through more retries than the maximum number of retries.
 *
 * Under the OAI protocol, the repository can also send back 302 Redirect responses, but the harvester doesn't currently
 * have a way to deal with that.
 *
 * The current implementation of the harvester is not intended to be used in a multithreaded environment.  Multiple
 * threads using a harvester, or even several harvesters, should be avoided anyway because the repository throttles
 * requests from the same machine/IP, so requests from multiple threads will create a lot of 503 Retry-After responses
 * and timeouts.  The OAI protocol was designed for bulk data update access anyway, not on-demand access.
 *
 * The current implementation of the harvester is blocking.  The thread invoking the harvest() method will be forced to
 * wait while the harvester sends the request, retrieves a (possibly lengthy) response, parses that response, and
 * complies with 503 Retry-After throttling.  This wait could last as much as
 *      minWaitBetweenRequests + (maxNumRetries - 1) * maxWaitBetweenRequests + local processing time.
 * A future update may change the implementation to be non-blocking, by resolving requests asynchronously with a request
 * queue and a single thread devoted to executing them.
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

    // Scale multiplier for retry wait times, to ensure we don't run afoul of the repository's throttling
    private static final double WAIT_PADDING = 1.1;

    private Instant lastResponseReceived;

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

    /**
     * Construct a harvester with user-specified settings.
     */
    public ArxivOAIHarvester(CloseableHttpClient httpClient,
                             int maxNumRetries,
                             Duration minWaitBetweenRequests,
                             Duration maxWaitBetweenRequests) {
        this(httpClient, new XMLParser(), maxNumRetries, minWaitBetweenRequests, maxWaitBetweenRequests);
    }

    @VisibleForTesting ArxivOAIHarvester(@NonNull CloseableHttpClient httpClient,
                             @NonNull XMLParser xmlParser,
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
        this.xmlParser = xmlParser;
        this.maxNumRetries = maxNumRetries;
        this.minWaitBetweenRequests = minWaitBetweenRequests;
        this.maxWaitBetweenRequests = maxWaitBetweenRequests;

        lastResponseReceived = Instant.MIN;
    }

    /**
     * See {@link #harvest(URI)} for exceptions.  Not thread-safe.
     */
    public GetRecordResponse harvest(@NonNull GetRecordRequest request) {
        ParsedXmlResponse xmlResponse = harvest(request.getUri());
        ArticleMetadata record = !xmlResponse.getRecords().isEmpty() ? xmlResponse.getRecords().get(0) : null;

        return GetRecordResponse.builder()
                .responseDate(xmlResponse.getResponseDate())
                .request(request)
                .record(record)
                .build();
    }

    /**
     * See {@link #harvest(URI)} for exceptions.  Not thread-safe.
     */
    public ListRecordsResponse harvest(@NonNull ListRecordsRequest request) {
        ParsedXmlResponse xmlResponse = harvest(request.getUri());

        ListRecordsResponse.ListRecordsResponseBuilder response =  ListRecordsResponse.builder()
                .responseDate(xmlResponse.getResponseDate());

        if (request instanceof ResumeListRecordsRequest) {
            response = response.request(((ResumeListRecordsRequest) request).getOriginalRequest());
        } else {
            response = response.request(request);
        }

        return response.records(ImmutableList.copyOf(xmlResponse.getRecords()))
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
     * @throws InterruptedError if the process is interrupted
     * @throws TimeoutException if there have been too many retries, or the repository has suggested a wait time that is too long
     * @throws ParseException if parsing fails
     * @throws RepositoryError if the repository's response was parseable but invalid
     * @throws BadArgumentException if the repository's response contains a BadArgument error
     * @throws BadResumptionTokenException if the repository's response contains a BadResumptionToken error
     */
    private ParsedXmlResponse harvest(@NonNull URI requestUri) {
        HttpGet httpRequest = new HttpGet(requestUri);
        if (userAgentHeader != null) {
            httpRequest.addHeader("User-Agent", userAgentHeader);
        }
        if (fromHeader != null) {
            httpRequest.addHeader("From", fromHeader);
        }

        // Before the first attempt, check if we are requesting too soon after the last request, and delay if necessary.
        Duration durationSinceLastResponseReceived = Duration.between(lastResponseReceived, Instant.now());
        if (durationSinceLastResponseReceived.compareTo(minWaitBetweenRequests) < 0) {
            Duration durationToWait = minWaitBetweenRequests.minus(durationSinceLastResponseReceived);
            log.info("Too soon since sending last request - waiting " + formatDurationSeconds(durationToWait) + " seconds...");
            try {
                Thread.sleep(durationToWait.toMillis());
            } catch (InterruptedException e) {
                log.error("Initial wait interrupted", e);
                throw new InterruptedError(e);
            }
        }

        RepositoryResponse response = tryHarvest(httpRequest);
        if (response.getParsedXmlResponse() != null) {
            return response.getParsedXmlResponse();
        }

        int numRetries = 1;
        Duration wait = response.getWait();
        while (numRetries <= maxNumRetries) {
            if (wait.compareTo(maxWaitBetweenRequests) > 0) {
                String errorString = "Repository-suggested wait time of " + formatDurationSeconds(wait) +
                        " exceeds maximum allowed wait time of " + formatDurationSeconds(maxWaitBetweenRequests) +
                        "; aborting request " + requestUri;
                log.warn(errorString);
                throw new TimeoutException(errorString);
            } else if (wait.compareTo(minWaitBetweenRequests) < 0) {
                wait = minWaitBetweenRequests;
            } else {
                // Padding to help ensure that we don't retry too early and run afoul of the repository's throttling logic
                double paddedWaitSeconds = WAIT_PADDING * wait.getSeconds();
                long paddedWaitMillis = (long) (paddedWaitSeconds * 1000);
                wait = Duration.ofMillis(paddedWaitMillis);
            }

            log.info("Waiting " + formatDurationSeconds(wait) + " seconds...");
            try {
                Thread.sleep(wait.toMillis());
            } catch (InterruptedException e) {
                log.error("Retry loop interrupted", e);
                throw new InterruptedError(e);
            }

            response = tryHarvest(httpRequest);
            if (response.getParsedXmlResponse() != null) {
                return response.getParsedXmlResponse();
            }

            numRetries++;
            wait = response.getWait();
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
            lastResponseReceived = Instant.now();
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
                    // don't really say what to do for most of them.  So we log and return an error response.
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
     * Nicely formats the number of seconds in a Duration.
     */
    private String formatDurationSeconds(Duration duration) {
        return String.format("%2.1f",
                BigDecimal.valueOf(duration.getSeconds()).add(BigDecimal.valueOf(duration.getNano(), 9)));
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
