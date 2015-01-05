package mikesaelim.arxivoaiharvester;

import lombok.NonNull;
import mikesaelim.arxivoaiharvester.data.ArticleMetadata;
import mikesaelim.arxivoaiharvester.data.ArticleVersion;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.List;
import java.util.Stack;

/**
 * Created by Mike Saelim on 1/5/15.
 */
public class XMLHandler extends DefaultHandler {

    /**
     * This list of records gets appended as we parse the XML document.  This is the output of the parser.
     */
    private List<ArticleMetadata> records;

    /**
     * At any point of the parsing process, this is a Stack of the XML nodes we are inside - the ones we have entered
     * but not yet exited.  When we enter a node, we push the name of that node onto the Stack, and when we leave a
     * node, we demand that its name match the one we pop off the stack.
     */
    private Stack<String> nodeStack;

    /**
     * The builder for the ArticleMetadata object currently being created.  This is only non-null while inside a record
     * node.
     */
    private ArticleMetadata.ArticleMetadataBuilder currentRecord;
    /**
     * The builder for the ArticleVersion object currently being created.  This is only non-null while inside a version
     * node.
     */
    private ArticleVersion.ArticleVersionBuilder currentVersion;


    /**
     * @param records list of records to be appended by the records parsed with this handler
     */
    public XMLHandler (@NonNull List<ArticleMetadata> records) {
        this.records = records;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        // TODO: implement
    }

    public void endElement(String uri, String localName, String qName) {
        // TODO: implement
    }

    public void characters(char[] ch, int start, int length) {
        // TODO: implement
    }

    // TODO: handle warnings, errors, etc.
}
