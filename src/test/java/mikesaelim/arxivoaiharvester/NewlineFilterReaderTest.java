package mikesaelim.arxivoaiharvester;

import org.junit.Before;
import org.junit.Test;

import java.io.FilterReader;
import java.io.StringReader;

import static org.junit.Assert.*;

public class NewlineFilterReaderTest {

    NewlineFilterReader reader;
    StringReader source;

    @Before
    public void setUp() throws Exception {
        source = new StringReader(" abc\n123");
        reader = new NewlineFilterReader(source);
    }

    @Test
    public void testRead() throws Exception {
        assertEquals(' ', (char) reader.read());
        assertEquals('a', (char) reader.read());
        assertEquals('b', (char) reader.read());
        assertEquals('c', (char) reader.read());
        assertEquals('1', (char) reader.read());
        assertEquals('2', (char) reader.read());
        assertEquals('3', (char) reader.read());
    }

    @Test
    public void testReadIntoBuffer() throws Exception {
        char[] cbuf = new char[7];
        int lengthFilled = reader.read(cbuf, 0, 7);

        assertEquals(7, lengthFilled);
        assertEquals(' ', cbuf[0]);
        assertEquals('a', cbuf[1]);
        assertEquals('b', cbuf[2]);
        assertEquals('c', cbuf[3]);
        assertEquals('1', cbuf[4]);
        assertEquals('2', cbuf[5]);
        assertEquals('3', cbuf[6]);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSkip() throws Exception {
        reader.skip(3);
    }
}