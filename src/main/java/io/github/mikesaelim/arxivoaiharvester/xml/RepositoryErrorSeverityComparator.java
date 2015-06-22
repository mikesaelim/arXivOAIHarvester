package io.github.mikesaelim.arxivoaiharvester.xml;

import com.google.common.collect.ImmutableMap;
import org.openarchives.oai._2.OAIPMHerrorType;
import org.openarchives.oai._2.OAIPMHerrorcodeType;

import java.util.Comparator;
import java.util.Map;

/**
 * Compares {@link OAIPMHerrorType} objects by the severity of their {@link OAIPMHerrorcodeType}.  Sorts by descending
 * severity.
 */
class RepositoryErrorSeverityComparator implements Comparator<OAIPMHerrorType> {

    /**
     * Map from OAI error codes to severity ranking.  Lower numbers are higher severity.
     *   1 - should never be received from a properly functioning repository, results in RepositoryError
     *   2 - bad input from user, results in a runtime exception
     *   3 - not an error
     */
    private static final Map<OAIPMHerrorcodeType, Integer> errorCodeSeverityMap =
            ImmutableMap.<OAIPMHerrorcodeType, Integer>builder()
                    .put(OAIPMHerrorcodeType.BAD_VERB, 1)
                    .put(OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT, 1)
                    .put(OAIPMHerrorcodeType.NO_METADATA_FORMATS, 1)
                    .put(OAIPMHerrorcodeType.NO_SET_HIERARCHY, 1)
                    .put(OAIPMHerrorcodeType.BAD_ARGUMENT, 2)
                    .put(OAIPMHerrorcodeType.BAD_RESUMPTION_TOKEN, 2)
                    .put(OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, 3)
                    .put(OAIPMHerrorcodeType.NO_RECORDS_MATCH, 3)
                    .build();



    @Override
    public int compare(OAIPMHerrorType o1, OAIPMHerrorType o2) {
        Integer severity1 = errorCodeSeverityMap.get(o1.getCode());
        Integer severity2 = errorCodeSeverityMap.get(o2.getCode());

        return severity1.compareTo(severity2);
    }

}
