package io.github.mikesaelim.arxivoaiharvester.io;

/**
 * OAI verbs, defined in the OAI-PMH specification, that are supported by this library.
 *
 * Created by Mike Saelim on 1/3/15.
 */
public enum Verb {
    GET_RECORD("GetRecord"),
    LIST_RECORDS("ListRecords");

    private final String uriFormat;

    Verb (String uriFormat) {
        this.uriFormat = uriFormat;
    }

    /**
     * Retrieve the verb in the format used when constructing the URI for a request.
     *
     * @return URI-formatted verb
     */
    public String getUriFormat() {
        return this.uriFormat;
    }

    public String toString() {
        return getUriFormat();
    }

    /**
     * Retrieve the Verb enum from a string representation.  This is not case-sensitive, and ignores leading and
     * trailing whitespace.
     *
     * @return the corresponding Verb, or if none match, null
     */
    public Verb fromString(String inputString) {
        if (inputString != null) {
            String inputStringLowercase = inputString.trim().toLowerCase();

            for (Verb verb : values()) {
                if (verb.toString().trim().toLowerCase().equals(inputStringLowercase)) {
                    return verb;
                }
            }
        }

        return null;
    }
}
