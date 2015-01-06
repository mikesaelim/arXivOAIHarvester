package mikesaelim.arxivoaiharvester;

import org.xml.sax.SAXException;
import mikesaelim.arxivoaiharvester.io.ArxivResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.xml.sax.Attributes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class XMLHandlerTest {

    private XMLHandler xmlHandler;

    @Mock
    private ArxivResponse.ArxivResponseBuilder responseBuilder;
    @Mock
    private Attributes attributes;

    @Before
    public void setUp() {
        initMocks(this);
        xmlHandler = new XMLHandler(responseBuilder);
    }

    @Test
    public void testParseVersionNumber() throws Exception {
        when(attributes.getValue(eq("version"))).thenReturn("v3");

        assertEquals(Integer.valueOf(3), xmlHandler.parseVersionNumber(attributes));
    }

    @Test(expected = SAXException.class)
    public void testParseVersionNumber_NoVersionShouldThrow() throws Exception {
        when(attributes.getValue(eq("version"))).thenReturn(null);

        xmlHandler.parseVersionNumber(attributes);
    }

    @Test(expected = SAXException.class)
    public void testParseVersionNumber_NoVShouldThrow() throws Exception {
        when(attributes.getValue(eq("version"))).thenReturn("3");

        xmlHandler.parseVersionNumber(attributes);
    }

    @Test(expected = SAXException.class)
    public void testParseVersionNumber_NoNumberShouldThrow() throws Exception {
        when(attributes.getValue(eq("version"))).thenReturn("vFaaaail");

        xmlHandler.parseVersionNumber(attributes);
    }

    @Test
    public void testParseResponseDate() throws Exception {
        String value = "2015-01-06T13:51:59Z";
        ZonedDateTime answer = ZonedDateTime.of(2015, 1, 6, 13, 51, 59, 0, ZoneId.of("Z"));

        assertEquals(answer, xmlHandler.parseResponseDate(value));
    }

    @Test(expected = SAXException.class)
    public void testParseResponseDate_BadFormatShouldThrow() throws Exception {
        xmlHandler.parseResponseDate("2015-01-T13:51:59Z");
    }

    @Test
    public void testParseDatestamp() throws Exception {
        String value = "2015-01-06";
        LocalDate answer = LocalDate.of(2015, 1, 6);

        assertEquals(answer, xmlHandler.parseDatestamp(value));
    }

    @Test(expected = SAXException.class)
    public void testParseDatestamp_BadFormatShouldThrow() throws Exception {
        xmlHandler.parseDatestamp("2015-01-");
    }

    @Test
    public void testParseVersionDate() throws Exception {
        String value = "Fri, 8 Feb 2013 21:00:01 GMT";
        ZonedDateTime answer = ZonedDateTime.of(2013, 2, 8, 21, 0, 1, 0, ZoneId.of("Z"));

        assertEquals(answer, xmlHandler.parseVersionDate(value));
    }

    @Test(expected = SAXException.class)
    public void testParseVersionDate_BadFormatShouldThrow() throws Exception {
        xmlHandler.parseVersionDate("Fri, 8 Feb 2013 21:00:01 GM");
    }

    @Test
    public void testParseCategories() throws Exception {
        String value = "quant-ph cond-mat.other hep-th math-ph math.CA math.MP nlin.SI";
        List<String> categories = xmlHandler.parseCategories(value);

        assertEquals(7, categories.size());
        assertEquals("quant-ph", categories.get(0));
        assertEquals("cond-mat.other", categories.get(1));
        assertEquals("hep-th", categories.get(2));
        assertEquals("math-ph", categories.get(3));
        assertEquals("math.CA", categories.get(4));
        assertEquals("math.MP", categories.get(5));
        assertEquals("nlin.SI", categories.get(6));
    }

    @Test
    public void testParseCategories_EmptyString() throws Exception {
        List<String> categories = xmlHandler.parseCategories("  ");
        assertTrue(categories.isEmpty());
    }






}