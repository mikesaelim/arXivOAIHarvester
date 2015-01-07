package mikesaelim.arxivoaiharvester;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.NonNull;
import mikesaelim.arxivoaiharvester.data.ArticleMetadata;
import mikesaelim.arxivoaiharvester.data.ArticleVersion;
import mikesaelim.arxivoaiharvester.io.ArxivResponse;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * This is the handler used to parse the XML response of arXiv's OAI repository.  It is written to be compatible with
 * the OAI-PMH v2.0 XML schema for the verbs "GetRecord" and "ListRecords" only, with metadata in arXiv's XML schema for
 * the "arXivRaw" metadata format (the current version of that is 2014-06-24).
 *
 * This handler is meant to be used with a SAX parser like so:
 * <pre>{@code
 *
 *  SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
 *
 *  ArxivResponse.ArxivResponseBuilder responseBuilder = ArxivResponse.builder();
 *  try {
 *      parser.parse(inputStream, new XMLHandler(responseBuilder));
 *  } catch (Exception e) {
 *      ...
 *  }
 *
 *  ArxivResponse response = responseBuilder.build();
 *
 * }</pre>
 * The inputStream gets parsed into the responseBuilder, which is used to build an immutable ArxivResponse object.
 *
 * Broad unit tests of this class's functionality are included in the unit tests of
 * {@link mikesaelim.arxivoaiharvester.ArxivOAIHarvester}.
 *
 * Created by Mike Saelim on 1/5/15.
 */
class XMLHandler extends DefaultHandler {

    // TODO: full checking of ancestor nodes
    // TODO: handle warnings, errors, etc.

    /**
     * This is the final output of the parsing process.  It gets filled when we hit the closing "GetRecord" or
     * "ListRecords" tag.
     */
    private ArxivResponse.ArxivResponseBuilder responseBuilder;

    /**
     * DateTime of the response.  The OAI v2.0 schema requires this to be filled before the records.
     */
    private ZonedDateTime responseDate;

    /**
     * This list of records gets appended as we parse the XML document.  This is initialized while entering a
     * "GetRecord" or "ListRecords" node.
     */
    private List<ArticleMetadata> records;

    /**
     * The identifier for the ArticleMetadata object currently being created.  This is only initialized while we are
     * inside a "record" node and we have come across the identifier already.  Unfortunately, because of how SAX parsing
     * works, there is no way to ensure that we will have an object's identifier even though we are parsing the object.
     *
     * This is basically just used for logging errors.
     */
    private String currentIdentifier;
    /**
     * The builder for the ArticleMetadata object currently being created.  This is initialized while entering a
     * "record" node.
     */
    private ArticleMetadata.ArticleMetadataBuilder currentRecordBuilder;
    /**
     * The Set of setSpec values for the ArticleMetadata object currently being created.  This is initialized while
     * entering a "record" node.
     */
    private Set<String> sets;
    /**
     * The Set of ArticleVersions for the ArticleMetadata object currently being created.  This is initialized while
     * entering a "record" node.
     */
    private Set<ArticleVersion> versions;
    /**
     * The builder for the ArticleVersion object currently being created.  This is initialized while entering a
     * "version" node.
     */
    private ArticleVersion.ArticleVersionBuilder currentVersionBuilder;
    /**
     * The StringBuilder for the abstract for the ArticleMetadata object currently being created.  This is initialized
     * while entering a "record" node.
     *
     * Unfortunately, the abstract is received with extra line breaks, and each line break triggers a new invocation of
     * characters(), so instead it must be built up piece-by-piece.  If we don't do it this way, and we simply assign
     * the string to currentRecordBuilder.articleAbstract(), only the very last line will get taken.
     */
    private StringBuilder articleAbstractBuilder;

    private String resumptionToken;
    private Integer cursor;
    private Integer completeListSize;

    /**
     * At any point of the parsing process, this is a Stack of the XML nodes we are inside - the ones we have entered
     * but not yet exited.  When we enter a node, we push the name of that node onto the Stack, and when we leave a
     * node, we demand that its name match the one we pop off the stack.
     * <p/>
     * Note: This is only active within "OAI-PMH" tags, so the base node should be "OAI-PMH".
     */
    private Stack<String> nodeStack;



    /**
     * @param responseBuilder ArxivReponse builder that will contain the output of this parsing
     */
    public XMLHandler (@NonNull ArxivResponse.ArxivResponseBuilder responseBuilder) {
        this.responseBuilder = responseBuilder;
    }

    /**
     * Code to be executed when encountering an opening tag
     */
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException{
        // Begin the nodeStack when we enter the OAI-PMH namespace
        if (qName.equals("OAI-PMH")) {
            if (nodeStack == null) {
                nodeStack = new Stack<>();
            } else {
                throw new SAXException(getIdentifierErrorString() + "Found nested <OAI-PMH> tags!");
            }
        }

        // Push this node onto the nodeStack
        nodeStack.push(qName);

        // Initialize containers as needed
        switch (qName) {
            case "GetRecord":
            case "ListRecords":
                records = Lists.newLinkedList();
                break;
            case "record":
                currentRecordBuilder = ArticleMetadata.builder().retrievalDateTime(responseDate);
                sets = Sets.newHashSet();
                versions = Sets.newHashSet();
                articleAbstractBuilder = new StringBuilder();
                currentIdentifier = null;
                break;
            case "header":
                currentRecordBuilder.deleted("deleted".equals(attributes.getValue("status")));
                break;
            case "version":
                currentVersionBuilder = ArticleVersion.builder()
                        .versionNumber(parseVersionNumber(attributes));
                break;
        }
    }

    /**
     * Code to be executed when encountering a closing tag
     */
    public void endElement(String uri, String localName, String qName) throws SAXException {
        // Pull the current node off the nodeStack
        String currentNode = nodeStack.pop();

        // If the closing tag doesn't match the last opening tag, throw an exception.
        if (!qName.equals(currentNode)) {
            throw new SAXException(getIdentifierErrorString() + "Opening tag '" + currentNode + "' does not match closing tag '" + qName + "'!");
        }

        // Close containers and insert them into their parent containers
        switch (qName) {
            case "GetRecord":
            case "ListRecords":
                responseBuilder.responseDate(responseDate)
                        .records(records)
                        .resumptionToken(resumptionToken)
                        .cursor(cursor)
                        .completeListSize(completeListSize);
                break;
            case "record":
                currentRecordBuilder.sets(sets)
                        .versions(versions)
                        .articleAbstract(articleAbstractBuilder.toString().trim());
                records.add(currentRecordBuilder.build());
                currentIdentifier = null;
                break;
            case "version":
                versions.add(currentVersionBuilder.build());
                break;
        }
    }

    /**
     * Parsing the text contents between opening and closing tags
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        String value = new String(ch, start, length);
        if (value.isEmpty()) {
            return;
        }

        switch (nodeStack.peek()) {
            case "responseDate":
                responseDate = parseResponseDate(value);
                break;

            // Begin record fields
            case "identifier":
                currentRecordBuilder.identifier(value);
                currentIdentifier = value;
                break;
            case "datestamp":
                currentRecordBuilder.datestamp(parseDatestamp(value));
                break;
            case "setSpec":
                sets.add(value);
                break;
            case "id":
                currentRecordBuilder.id(value);
                break;
            case "submitter":
                currentRecordBuilder.submitter(value);
                break;

            // Begin version fields
            case "date":
                currentVersionBuilder.submissionTime(parseVersionDate(value));
                break;
            case "size":
                currentVersionBuilder.size(value);
                break;
            case "source_type":
                currentVersionBuilder.sourceType(value);
                break;
            // End version fields

            case "title":
                currentRecordBuilder.title(value);
                break;
            case "authors":
                currentRecordBuilder.authors(value);
                break;
            case "categories":
                currentRecordBuilder.categories(parseCategories(value));
                break;
            case "comments":
                currentRecordBuilder.comments(value);
                break;
            case "proxy":
                currentRecordBuilder.proxy(value);
                break;
            case "report-no":
                currentRecordBuilder.reportNo(value);
                break;
            case "acm-class":
                currentRecordBuilder.acmClass(value);
                break;
            case "msc-class":
                currentRecordBuilder.mscClass(value);
                break;
            case "journal-ref":
                currentRecordBuilder.journalRef(value);
                break;
            case "doi":
                currentRecordBuilder.doi(value);
                break;
            case "license":
                currentRecordBuilder.license(value);
                break;
            case "abstract":
                articleAbstractBuilder.append(value.replace('\n', ' '));
                break;
            // End record fields
        }
    }



    /**
     * Parse the version number from the attributes of the "version" node.  Per the arXivRaw XML schema, this should be
     * in the form "v1", "v2", etc.
     * @param attributes "version" node attributes
     * @return version number
     * @throws SAXException if the label cannot be found or does not fit the specified format
     */
    @VisibleForTesting Integer parseVersionNumber(Attributes attributes) throws SAXException {
        String versionLabel = attributes.getValue("version");

        if (versionLabel == null) {
            throw new SAXException(getIdentifierErrorString() + "Could not find version label!");
        }
        if (!versionLabel.startsWith("v")) {
            throw new SAXException(getIdentifierErrorString() + "Could not parse version label '" + versionLabel + "'!");
        }

        Integer version;
        try {
            version = Integer.valueOf(versionLabel.substring(1));
        } catch (NumberFormatException e) {
            throw new SAXException(getIdentifierErrorString() + "Could not parse version label '" + versionLabel + "'!");
        }

        return version;
    }

    /**
     * Parse the responseDate of an OAI response.
     * @throws SAXException if there is a parsing error
     */
    @VisibleForTesting ZonedDateTime parseResponseDate(String value) throws SAXException {
        ZonedDateTime responseDate;
        try {
            responseDate = ZonedDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new SAXException(getIdentifierErrorString() + "Could not parse responseDate '" + value + "' in ISO_OFFSET_DATE_TIME format!");
        }
        return responseDate;
    }

    /**
     * Parse the datestamp of a record.
     * @throws SAXException if there is a parsing error
     */
    @VisibleForTesting LocalDate parseDatestamp(String value) throws SAXException {
        LocalDate datestamp;
        try {
            datestamp = LocalDate.parse(value);
        } catch(DateTimeParseException e) {
            throw new SAXException(getIdentifierErrorString() + "Could not parse datestamp '" + value + "' in ISO_LOCAL_DATE format!");
        }
        return datestamp;
    }

    /**
     * Parse the date of an article version.
     * @throws SAXException if there is a parsing error
     */
    @VisibleForTesting ZonedDateTime parseVersionDate(String value) throws SAXException {
        ZonedDateTime versionDate;
        try {
            versionDate = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new SAXException(getIdentifierErrorString() + "Could not parse version date '" + value + "' in RFC_1123_DATE_TIME format!");
        }
        return versionDate;
    }

    /**
     * Parse the category string of an article.
     * @return List of separate categories, in the same order as they were in the string
     */
    @VisibleForTesting List<String> parseCategories(String value) {
        if (value == null) {
            return Lists.newArrayList();
        }
        return Lists.newArrayList(value.split(" "));
    }

    /**
     * Return a String containing the identifier of the record currently being processed, if it exists, to be prepended
     * on any error messages.  This will aid in debugging.
     */
    private String getIdentifierErrorString() {
        if (currentIdentifier != null) {
            return "identifier = '" + currentIdentifier + "': ";
        }

        return "";
    }







}
