package io.github.mikesaelim.arxivoaiharvester.model.data;

import lombok.Value;
import lombok.experimental.Builder;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

/**
 * Metadata for an article, based on all the data returned in the OAI record.  If a field was not included in the OAI
 * response for this record, it is null.  Immutable.
 */
@Value
@Builder
public class ArticleMetadata {

    /**
     * Time when this metadata was retrieved from the arXiv OAI repository.
     */
    private ZonedDateTime retrievalDateTime;




    /////// HEADER DATA ////////

    /**
     * Unique identifier string of this record.
     */
    private String identifier;

    /**
     * Datestamp of when this record was last updated in the repository.
     */
    private LocalDate datestamp;

    /**
     * Sets that this record belongs to.  Because of arXiv's OAI peculiarities, this may be an incomplete list if this
     * ArticleMetadata was retrieved with a request that restricted the results to a single set.
     */
    private Set<String> sets;

    /**
     * True if the record has been deleted from the repository.  If so, there is no metadata other than the identifier,
     * datestamp, and sets.
     */
    private boolean deleted;




    /////// ARTICLE METADATA ///////

    /**
     * arXiv ID for the article.
     */
    private String id;

    /**
     * Name of the original (version 1) submitter.
     */
    private String submitter;

    /**
     * Set of metadata about each of the article versions submitted.
     */
    private Set<ArticleVersion> versions;

    /**
     * Title of the article.
     */
    private String title;

    /**
     * Author string of the article.  Unfortunately, this is not really in any sort of fixed format.
     */
    private String authors;

    /**
     * Categories of the article.  The list should be kept in the same order as they appeared in the category string
     * of the record, so the first category should be the primary category of the article.
     *
     * @see <a href="http://arxiv.org/help/prep#subj">http://arxiv.org/help/prep#subj</a>
     */
    private List<String> categories;

    /**
     * Comments string of the article.
     */
    private String comments;

    /**
     * Proxy string.  Used only for third-party submissions: see
     * <a href="http://arxiv.org/help/third_party_submission">the relevant arXiv help page</a>.
     */
    private String proxy;

    /**
     * Report number.  Usually used for an author's institution's internal tracking system.
     */
    private String reportNo;

    /**
     * ACM class, as defined by the <a href="http://www.acm.org/about/class/">Association for Computing Machinery</a>.
     * Usually used in CS archives.
     */
    private String acmClass;

    /**
     * MSC class, as defined by the <a href="http://www.ams.org/msc/">Mathematics Subject Classification</a>.
     * Usually used in math archives.
     */
    private String mscClass;

    /**
     * Reference to the associated published journal article.
     */
    private String journalRef;

    /**
     * DOI(s) for published or other versions of this article.
     */
    private String doi;

    /**
     * License string.
     */
    private String license;

    /**
     * Abstract of the article.  It's not called 'abstract' because that's a Java keyword.
     */
    private String articleAbstract;

}
