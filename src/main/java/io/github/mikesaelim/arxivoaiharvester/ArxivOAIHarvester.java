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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;

/**
 * TODO javadoc
 *
 * This class is most definitely not thread-safe.
 */
@Slf4j
public class ArxivOAIHarvester {

    private final CloseableHttpClient httpClient;
    private final XMLParser xmlParser;

    private final String userAgentHeader;
    private final String fromHeader;

    public ArxivOAIHarvester(CloseableHttpClient httpClient, String userAgentHeader, String fromHeader) {
        // TODO add harvester control flow params
        this.httpClient = httpClient;
        this.userAgentHeader = userAgentHeader;
        this.fromHeader = fromHeader;

        this.xmlParser = new XMLParser();
    }

    /**
     * See {@link #harvest(URI)} for exceptions.
     */
    public GetRecordResponse harvest(@NonNull GetRecordRequest request) {
        ParsedXmlResponse xmlResponse = harvest(request.getUri());

        return GetRecordResponse.builder()
                .responseDate(xmlResponse.getResponseDate())
                .request(request)
                .record(xmlResponse.getRecords().get(0))
                .build();
    }

    /**
     * See {@link #harvest(URI)} for exceptions.
     */
    public ListRecordsResponse harvest(@NonNull ListRecordsRequest request) {
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
     * See {@link #harvest(URI)} for exceptions.
     */
    public ListRecordsResponse harvest(@NonNull ResumeListRecordsRequest request) {
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
     * @param requestUri URI to be sent to the repository
     * @return parsed content of the response from the repository
     *
     * @throws NullPointerException if requestUri is null
     * @throws BadArgumentException if the repository's response contains a BadArgument error
     * @throws BadResumptionTokenException if the repository's response contains a BadResumptionToken error
     * @throws ParseException if parsing fails
     * @throws RepositoryError if the repository's response was parseable but invalid
     */
    private ParsedXmlResponse harvest(@NonNull URI requestUri) {
        // TODO control flow and 503

        HttpGet httpRequest = new HttpGet(requestUri);
        httpRequest.addHeader("User-Agent", userAgentHeader);
        httpRequest.addHeader("From", fromHeader);

        log.info("Sending request to arXiv OAI repository: {}", requestUri);

        ParsedXmlResponse parsedXmlResponse;
        try (CloseableHttpResponse httpResponse = httpClient.execute(httpRequest)) {
            int httpStatusCode = httpResponse.getStatusLine().getStatusCode();

            switch (httpStatusCode) {
                case HttpStatus.SC_OK:
                    log.info("Parsing response from arXiv OAI repository for request {}", requestUri);

                    try {
                        parsedXmlResponse = xmlParser.parse(httpResponse.getEntity().getContent());
                    } catch (BadArgumentException | BadResumptionTokenException e) {
                        log.error(String.format("Repository complained about input for request %s", requestUri), e);
                        throw e;
                    } catch (ParseException | RepositoryError e) {
                        log.error(String.format("Error parsing response for request %s", requestUri), e);
                        throw e;
                    }

                    log.info("Response parsed for request {}", requestUri);

                    return parsedXmlResponse;
                case HttpStatus.SC_MOVED_TEMPORARILY:
                    // Handling this is not currently supported
                    String movedErrorString = "Redirect received for request " + requestUri;
                    log.error(movedErrorString);
                    throw new UnsupportedRedirectException(movedErrorString);
                case HttpStatus.SC_NOT_FOUND:
                    String notFoundErrorString = "Received 404 for request " + requestUri;
                    log.error(notFoundErrorString);
                    throw new RepositoryError(notFoundErrorString);
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    // TODO: Handle 503
                default:
                    // Unfortunately, we currently aren't prepared to handle other HTTP status codes.  The OAI specs
                    // don't really say what to do for most of them.  So we log a warning and return an error response.
                    String defaultErrorString = "Returned status code " + httpStatusCode + ": " +
                            httpResponse.getStatusLine().getReasonPhrase() + ": " +
                            EntityUtils.toString(httpResponse.getEntity());
                    log.error(defaultErrorString);
                    throw new RepositoryError(defaultErrorString);
            }
        } catch (IOException | IllegalStateException e) {
            log.error("Error retrieving response from arXivOAI repository", e);
            throw new HttpException(e);
        }

    }

}
