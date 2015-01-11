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

import static org.apache.commons.lang3.StringUtils.normalizeSpace;

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

    private String resumptionToken;
    private Integer cursor;
    private Integer completeListSize;

    /**
     * A StringBuilder used to construct the value of the current leaf node.  The resulting String will get parsed and
     * stored in the fields above.
     *
     * Unfortunately, the XML stream is received with extra spurious line breaks inside the field values, and some of
     * the line breaks trigger a new invocation of the characters() method.  We deal with this corrupted XML by building
     * the value Strings "line" by "line".  Sigh.
     */
    private StringBuilder currentValueBuilder;
    private static final Set<String> LEAF_NODE_NAMES = Sets.newHashSet("responseDate", "resumptionToken", "identifier",
            "datestamp", "setSpec", "id", "submitter", "date", "size", "source_type", "title", "authors", "categories",
            "comments", "proxy", "report-no", "acm-class", "msc-class", "journal-ref", "doi", "license", "abstract");

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
    @Override
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
                cursor = parseCursor(attributes);
                completeListSize = parseCompleteListSize(attributes);
                // The lack of a break statement here is intentional
            default:
                currentValueBuilder = new StringBuilder();
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
                        .versions(versions);
                records.add(currentRecordBuilder.build());
                currentIdentifier = null;
                break;
            case "version":
                versions.add(currentVersionBuilder.build());
                break;

            // Begin leaf nodes
            case "responseDate":
                responseDate = parseResponseDate(normalizeSpace(currentValueBuilder.toString()));
                break;
            case "resumptionToken":
                resumptionToken = normalizeSpace(currentValueBuilder.toString());
                break;

            // Begin record fields
            case "identifier":
                currentIdentifier = normalizeSpace(currentValueBuilder.toString());
                currentRecordBuilder.identifier(currentIdentifier);
                break;
            case "datestamp":
                currentRecordBuilder.datestamp(parseDatestamp(normalizeSpace(currentValueBuilder.toString())));
                break;
            case "setSpec":
                sets.add(normalizeSpace(currentValueBuilder.toString()));
                break;
            case "id":
                currentRecordBuilder.id(normalizeSpace(currentValueBuilder.toString()));
                break;
            case "submitter":
                currentRecordBuilder.submitter(normalizeSpace(currentValueBuilder.toString()));
                break;

            // Begin version fields
            case "date":
                currentVersionBuilder.submissionTime(parseVersionDate(normalizeSpace(currentValueBuilder.toString())));
                break;
            case "size":
                currentVersionBuilder.size(normalizeSpace(currentValueBuilder.toString()));
                break;
            case "source_type":
                currentVersionBuilder.sourceType(normalizeSpace(currentValueBuilder.toString()));
                break;
            // End version fields

            case "title":
                currentRecordBuilder.title(normalizeSpace(currentValueBuilder.toString()));
                break;
            case "authors":
                currentRecordBuilder.authors(normalizeSpace(currentValueBuilder.toString()));
                break;
            case "categories":
                currentRecordBuilder.categories(parseCategories(normalizeSpace(currentValueBuilder.toString())));
                break;
            case "comments":
                currentRecordBuilder.comments(normalizeSpace(currentValueBuilder.toString()));
                break;
            case "proxy":
                currentRecordBuilder.proxy(normalizeSpace(currentValueBuilder.toString()));
                break;
            case "report-no":
                currentRecordBuilder.reportNo(normalizeSpace(currentValueBuilder.toString()));
                break;
            case "acm-class":
                currentRecordBuilder.acmClass(normalizeSpace(currentValueBuilder.toString()));
                break;
            case "msc-class":
                currentRecordBuilder.mscClass(normalizeSpace(currentValueBuilder.toString()));
                break;
            case "journal-ref":
                currentRecordBuilder.journalRef(normalizeSpace(currentValueBuilder.toString()));
                break;
            case "doi":
                currentRecordBuilder.doi(normalizeSpace(currentValueBuilder.toString()));
                break;
            case "license":
                currentRecordBuilder.license(normalizeSpace(currentValueBuilder.toString()));
                break;
            case "abstract":
                currentRecordBuilder.articleAbstract(normalizeSpace(currentValueBuilder.toString()));
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

        if (LEAF_NODE_NAMES.contains(nodeStack.peek())) {
            currentValueBuilder.append(value);
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
                // Do nothing, although in the future this will be used for logging
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
                // Do nothing, although in the future this will be used for logging
            }
        }

        return completeListSizeAmount;
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
