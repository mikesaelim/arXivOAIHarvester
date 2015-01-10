package mikesaelim.arxivoaiharvester;

import com.sun.tools.doclets.formats.html.SourceToHTMLConverter;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Stream reader that filters newlines (\n, \r) out.  Used for parsing the corrupted XML response from arXiv's OAI
 * repository, which introduces spurious newlines in the middle of strings.  Ugh.
 *
 * Created by Mike Saelim on 1/9/15.
 */
public class NewlineFilterReader extends FilterReader {

    /**
     * Construct a new NewlineFilterReader
     * @param in input Reader
     */
    public NewlineFilterReader(Reader in) {
        super(in);
    }

    /**
     * Read a single character of the filtered stream
     * @return the character, cast as an integer
     * @throws IOException
     */
    @Override
    public int read() throws IOException {
        char[] cbuf = new char[1];
        System.out.println("start new read");
        int lengthFilled = read(cbuf, 0, 1);
        if (lengthFilled == 0) {
            // We encountered the end of stream
            return -1;
        }
        return (int) cbuf[0];
    }

    /**
     * Read a segment of the filtered stream into a character buffer
     * @param cbuf character buffer
     * @param off offset of where to start writing into the character buffer
     * @param len maximum number of characters to write into the character buffer
     * @return number of characters written into the character buffer
     * @throws IOException
     * @throws IndexOutOfBoundsException
     */
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len < 0 || off + len > cbuf.length) {
            throw new IndexOutOfBoundsException();
        }

        System.out.println("start new readbuffer");

        int lengthFilled = 0;
        while (lengthFilled < len) {
            int characterAsInt = super.read();

            System.out.println("buh: " + lengthFilled + ", moo: " + characterAsInt);

            // -1 means end of stream, and we cannot cast that to a char
            if (characterAsInt == -1) {

                System.out.println("out!");

                return -1;
            }

            char characterAsChar = (char) characterAsInt;
            if (characterPasses(characterAsChar)) {

                System.out.println("eee: " + characterAsChar);

                cbuf[off+lengthFilled] = characterAsChar;
                lengthFilled++;
            }
        }

        return lengthFilled;
    }

    /**
     * This method is affected by the filtering logic, but since ReaderInputStream does not seem to use it, I have
     * decided not to implement it.
     * @throws UnsupportedOperationException
     */
    @Override
    public long skip(long n) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return true if character passes the filter
     */
    private boolean characterPasses(char character) {
        return !(character == '\n' || character == '\r');
    }

}
