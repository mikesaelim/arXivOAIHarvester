package mikesaelim.arxivoaiharvester;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.NonNull;
import mikesaelim.arxivoaiharvester.data.ArticleMetadata;
import mikesaelim.arxivoaiharvester.data.ArticleVersion;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * This handler is meant to be used with a SAXParser, which parses an InputStream and sends the information to this
 * handler.  This handler then parses this information and stores it in a ParsedXMLResponse.  Example:
 * <pre>{@code
 *    ParsedXmlResponse parsedXmlResponse = new ParsedXmlResponse();
 *    try {
 *        saxParser.parse(inputStream, new XMLHandler(parsedXmlResponse));
 *    } catch (Exception e) { ... }
 * }</pre>
 *
 * ArticleMetadata and ArticleVersion are immutable types, so they are constructed via builders.  Additionally, we have
 * to deal with a corrupted XML stream that contains spurious line breaks in the middle of some of the string values.
 * For this reason, we normalize the string values that we extract.
 *
 * Broad unit tests of this class's functionality are included in the unit tests of ArxivOAIHarvester.
 *
 * Created by Mike Saelim on 1/5/15.
 */
class XMLHandler extends DefaultHandler {

    // TODO: handle warnings, errors, etc. from the parser
    // TODO: handle error responses from the repository

    Logger log = LoggerFactory.getLogger(XMLHandler.class);

    /**
     * This is the final output, which gets filled as the XML is parsed.
     */
    private ParsedXmlResponse parsedXmlResponse;

    /*
     *        These are objects that get filled anew for every record the parser encounters:
     */
    /**
     * The unique identifier for the record being parsed.  It is stored separately so that it can be used for logging
     * errors.
     */
    private String currentIdentifier;
    /**
     * The ArticleMetadata builder for the record being parsed.
     */
    private ArticleMetadata.ArticleMetadataBuilder currentRecordBuilder;
    /**
     * The Set of "setSpec" values for the record being parsed.
     */
    private Set<String> sets;
    /**
     * The Set of ArticleVersions for the record being parsed.
     */
    private Set<ArticleVersion> versions;

    /*
     *       These are objects that get filled anew for every "version" in a record:
     */
    /**
     * The builder for the ArticleVersion object currently being created.
     */
    private ArticleVersion.ArticleVersionBuilder currentVersionBuilder;

    /*
     *       These are objects that are used to track the current state of the parsing:
     */
    /**
     * At any point of the parsing process, this is a Stack of the XML nodes we are inside - the ones we have entered
     * but not yet exited.  When we enter a node, we push the name of that node onto the Stack, and when we leave a
     * node, we demand that its name match the one we pop off the stack.
     *
     * This is only active within "OAI-PMH" tags, so the base node should be "OAI-PMH".
     */
    private Stack<String> nodeStack;
    /**
     * A StringBuilder used to construct the value of the current leaf node.  The resulting String will get parsed and
     * stored in the fields above.
     *
     * Unfortunately, the XML stream is received with extra spurious line breaks inside the field values, and some of
     * the line breaks trigger a new invocation of the characters() method.  We deal with this corrupted XML by building
     * the value Strings "line" by "line".  Sigh.
     */
    private StringBuilder currentLeafValueBuilder;
    private static final Set<String> LEAF_NAMES = Sets.newHashSet("responseDate", "resumptionToken", "identifier",
            "datestamp", "setSpec", "id", "submitter", "date", "size", "source_type", "title", "authors", "categories",
            "comments", "proxy", "report-no", "acm-class", "msc-class", "journal-ref", "doi", "license", "abstract");



    /**
     * @param parsedXmlResponse container for the parsing output
     */
    public XMLHandler (@NonNull ParsedXmlResponse parsedXmlResponse) {
        this.parsedXmlResponse = parsedXmlResponse;
        this.parsedXmlResponse.setRecords(Lists.newArrayList());
    }

    /**
     * Code to be executed when encountering an opening tag
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException{
        // Begin the nodeStack when we enter the OAI-PMH namespace
        if (qName.equals("OAI-PMH")) {
            if (nodeStack == null) {
                nodeStack = new Stack<>();
            } else {
                logNodeStack();
                throw new SAXException(getIdentifierErrorString() + "Found nested <OAI-PMH> tags!");
            }
        }

        // Push this node onto the nodeStack
        nodeStack.push(qName);

        // Initialize containers as needed
        switch (qName) {
            case "GetRecord":
            case "ListRecords":
                // Do nothing - parsedXmlResponse.records has already been initialized in the constructor
                break;
            case "record":
                currentRecordBuilder = ArticleMetadata.builder().retrievalDateTime(parsedXmlResponse.getResponseDate());
                sets = Sets.newHashSet();
                versions = Sets.newHashSet();
                currentIdentifier = null;
                break;
            case "header":
                currentRecordBuilder.deleted("deleted".equals(attributes.getValue("status")));
                break;
            case "version":
                currentVersionBuilder = ArticleVersion.builder()
                        .versionNumber(parseVersionNumber(attributes));
                break;
            case "resumptionToken":
                parsedXmlResponse.setCursor(parseCursor(attributes));
                parsedXmlResponse.setCompleteListSize(parseCompleteListSize(attributes));
                currentLeafValueBuilder = new StringBuilder();
                break;
            default:
                currentLeafValueBuilder = new StringBuilder();
        }
    }

    /**
     * Code to be executed when encountering a closing tag
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        // Pull the current node off the nodeStack
        String currentNode = nodeStack.pop();

        // If the closing tag doesn't match the last opening tag, throw an exception.
        if (!qName.equals(currentNode)) {
            logNodeStack();
            throw new SAXException(getIdentifierErrorString() + "Opening tag '" + currentNode +
                    "' does not match closing tag '" + qName + "'!");
        }

        // Build leaf node values and close containers, and insert them into their parent containers
        switch (qName) {
            case "GetRecord":
            case "ListRecords":
                // Do nothing - data has already been stored in parsedXmlResponse
                break;
            case "record":
                currentRecordBuilder.sets(sets).versions(versions);
                parsedXmlResponse.getRecords().add(currentRecordBuilder.build());
                currentIdentifier = null;
                break;
            case "version":
                versions.add(currentVersionBuilder.build());
                break;

            // Begin leaf nodes - their values are retrieved from getCurrentValue()
            case "responseDate":
                parsedXmlResponse.setResponseDate(parseResponseDate(getCurrentValue()));
                break;
            case "resumptionToken":
                parsedXmlResponse.setResumptionToken(getCurrentValue());
                break;

            // Begin record fields
            case "identifier":
                currentIdentifier = getCurrentValue();
                currentRecordBuilder.identifier(currentIdentifier);
                break;
            case "datestamp":
                currentRecordBuilder.datestamp(parseDatestamp(getCurrentValue()));
                break;
            case "setSpec":
                sets.add(getCurrentValue());
                break;
            case "id":
                currentRecordBuilder.id(getCurrentValue());
                break;
            case "submitter":
                currentRecordBuilder.submitter(getCurrentValue());
                break;

            // Begin version fields
            case "date":
                currentVersionBuilder.submissionTime(parseVersionDate(getCurrentValue()));
                break;
            case "size":
                currentVersionBuilder.size(getCurrentValue());
                break;
            case "source_type":
                currentVersionBuilder.sourceType(getCurrentValue());
                break;
            // End version fields

            case "title":
                currentRecordBuilder.title(getCurrentValue());
                break;
            case "authors":
                currentRecordBuilder.authors(getCurrentValue());
                break;
            case "categories":
                currentRecordBuilder.categories(parseCategories(getCurrentValue()));
                break;
            case "comments":
                currentRecordBuilder.comments(getCurrentValue());
                break;
            case "proxy":
                currentRecordBuilder.proxy(getCurrentValue());
                break;
            case "report-no":
                currentRecordBuilder.reportNo(getCurrentValue());
                break;
            case "acm-class":
                currentRecordBuilder.acmClass(getCurrentValue());
                break;
            case "msc-class":
                currentRecordBuilder.mscClass(getCurrentValue());
                break;
            case "journal-ref":
                currentRecordBuilder.journalRef(getCurrentValue());
                break;
            case "doi":
                currentRecordBuilder.doi(getCurrentValue());
                break;
            case "license":
                currentRecordBuilder.license(getCurrentValue());
                break;
            case "abstract":
                currentRecordBuilder.articleAbstract(getCurrentValue());
                break;
            // End record fields
        }
    }

    /**
     * Parsing the text contents between opening and closing tags
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String value = new String(ch, start, length);
        if (value.isEmpty()) {
            return;
        }

        if (LEAF_NAMES.contains(nodeStack.peek())) {
            currentLeafValueBuilder.append(value);
        }
    }


    /**
     * Return the value of the leaf node that we are currently in.  This value may have been built up from multiple
     * lines, and is properly normalized.  Also invalidate the used StringBuilder.
     * @throws SAXException if currentLeafValueBuilder is null
     */
    private String getCurrentValue() throws SAXException {
        if (currentLeafValueBuilder == null) {
            logNodeStack();
            throw new SAXException(getIdentifierErrorString() +
                    "Attempted to retrieve leaf node value from a null currentLeafValueBuilder!");
        }

        String currentValue = StringUtils.normalizeSpace(currentLeafValueBuilder.toString());
        currentLeafValueBuilder = null;
        return currentValue;
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
            logNodeStack();
            throw new SAXException(getIdentifierErrorString() + "Could not find version label!");
        }
        if (!versionLabel.startsWith("v")) {
            logNodeStack();
            throw new SAXException(getIdentifierErrorString() + "Could not parse version label '" + versionLabel + "'!");
        }

        Integer version;
        try {
            version = Integer.valueOf(versionLabel.substring(1));
        } catch (NumberFormatException e) {
            logNodeStack();
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
            logNodeStack();
            throw new SAXException(getIdentifierErrorString() + "Could not parse responseDate '" + value +
                    "' in ISO_OFFSET_DATE_TIME format!");
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
            logNodeStack();
            throw new SAXException(getIdentifierErrorString() + "Could not parse datestamp '" + value +
                    "' in ISO_LOCAL_DATE format!");
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
            logNodeStack();
            throw new SAXException(getIdentifierErrorString() + "Could not parse version date '" + value +
                    "' in RFC_1123_DATE_TIME format!");
        }
        return versionDate;
    }

    /**
     * Parse the category string of an article.
     * @return List of separate categories, in the same order as they were in the string
     */
    @VisibleForTesting List<String> parseCategories(String value) {
        return value != null ? Lists.newArrayList(value.split(" ")) : Lists.newArrayList();
    }

    /**
     * Parse the cursor from the attributes of the "resumptionToken" node.  This attribute is not required by the OAI
     * schema, so we do not throw an exception if we cannot find it.
     * @param attributes "resumptionToken" node attributes
     * @return cursor position, or null if no valid one can be found
     */
    @VisibleForTesting Integer parseCursor(Attributes attributes) {
        String cursorString = attributes.getValue("cursor");

        Integer cursorPosition = null;
        if (cursorString != null) {
            try {
                cursorPosition = Integer.valueOf(cursorString);
            } catch (NumberFormatException e) {
                log.warn("Cursor not found for resumption token!");
            }
        }

        return cursorPosition;
    }

    /**
     * Parse the complete list size from the attributes of the "resumptionToken" node.  This attribute is not required
     * by the OAI schema, so we do not throw an exception if we cannot find it.
     * @param attributes "resumptionToken" node attributes
     * @return complete list size, or null if no valid one can be found
     */
    @VisibleForTesting Integer parseCompleteListSize(Attributes attributes) {
        String completeListSizeString = attributes.getValue("completeListSize");

        Integer completeListSizeAmount = null;
        if (completeListSizeString != null) {
            try {
                completeListSizeAmount = Integer.valueOf(completeListSizeString);
            } catch (NumberFormatException e) {
                log.warn("Complete list size not found for resumption token!");
            }
        }

        return completeListSizeAmount;
    }

    /**
     * Log the current state of the node stack, in the case of an error.  This will aid in debugging by telling us where
     * the error occurred.
     */
    private void logNodeStack() {
        if (nodeStack != null) {
            log.error("Error in node stack: " + nodeStack.toString());
        }
    }

    /**
     * Return the identifier of the record currently being processed, if it exists, to be prepended to any error
     * messages.  This will aid in debugging by telling us where the error occurred.
     */
    private String getIdentifierErrorString() {
        if (currentIdentifier != null) {
            return "identifier = '" + currentIdentifier + "': ";
        }

        return "";
    }

}
