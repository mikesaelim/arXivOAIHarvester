package io.github.mikesaelim.arxivoaiharvester;

import io.github.mikesaelim.arxivoaiharvester.model.request.GetRecordRequest;
import io.github.mikesaelim.arxivoaiharvester.model.request.ListRecordsRequest;
import io.github.mikesaelim.arxivoaiharvester.model.request.ResumeListRecordsRequest;
import io.github.mikesaelim.arxivoaiharvester.model.response.GetRecordResponse;
import io.github.mikesaelim.arxivoaiharvester.model.response.ListRecordsResponse;
import io.github.mikesaelim.arxivoaiharvester.xml.ParsedXmlResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;

import java.net.URI;

/**
 * TODO javadoc
 * TODO see if we need separate harvesters?
 *
 * This class is most definitely not thread-safe.
 */
@Slf4j
public class ArxivOAIHarvester {

    private final CloseableHttpClient httpClient;

    public ArxivOAIHarvester(CloseableHttpClient httpClient) {  // TODO add harvester control flow params
        this.httpClient = httpClient;
    }

    public GetRecordResponse harvest(GetRecordRequest request) {
        return null;
    }

    public ListRecordsResponse harvest(ListRecordsRequest request) {
        return null;
    }

    public ListRecordsResponse harvest(ResumeListRecordsRequest request) {
        return null;
    }

    private ParsedXmlResponse harvest(URI requestUri) {
        return null;
    }
}
