package io.github.mikesaelim.arxivoaiharvester.xml;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class XMLParserTest {

    private XMLParser xmlParser;

    @Before
    public void setUp() {
        initMocks(this);
        xmlParser = new XMLParser();
    }


/*
    @Test
    public void testParseVersionNumber() throws Exception {
        when(attributes.getValue(eq("version"))).thenReturn("v3");

        assertEquals(Integer.valueOf(3), xmlParser.parseVersionNumber(attributes));
    }

    @Test(expected = SAXException.class)
    public void testParseVersionNumber_NoVersionShouldThrow() throws Exception {
        when(attributes.getValue(eq("version"))).thenReturn(null);

        xmlParser.parseVersionNumber(attributes);
    }

    @Test(expected = SAXException.class)
    public void testParseVersionNumber_NoVShouldThrow() throws Exception {
        when(attributes.getValue(eq("version"))).thenReturn("3");

        xmlParser.parseVersionNumber(attributes);
    }

    @Test(expected = SAXException.class)
    public void testParseVersionNumber_NoNumberShouldThrow() throws Exception {
        when(attributes.getValue(eq("version"))).thenReturn("vFaaaail");

        xmlParser.parseVersionNumber(attributes);
    }



    @Test
    public void testParseResponseDate() throws Exception {
        String value = "2015-01-06T13:51:59Z";
        ZonedDateTime answer = ZonedDateTime.of(2015, 1, 6, 13, 51, 59, 0, ZoneId.of("Z"));

        assertEquals(answer, xmlParser.parseResponseDate(value));
    }

    @Test(expected = SAXException.class)
    public void testParseResponseDate_BadFormatShouldThrow() throws Exception {
        xmlParser.parseResponseDate("2015-01-T13:51:59Z");
    }



    @Test
    public void testParseError() throws Exception {
        Map<String, ArxivError.Type> solutionMap = Maps.newHashMap();
        solutionMap.put("badArgument", ArxivError.Type.ILLEGAL_ARGUMENT);
        solutionMap.put("badResumptionToken", ArxivError.Type.INTERNAL_ERROR);
        solutionMap.put("badVerb", ArxivError.Type.INTERNAL_ERROR);
        solutionMap.put("cannotDisseminateFormat", ArxivError.Type.INTERNAL_ERROR);
        solutionMap.put("idDoesNotExist", null);
        solutionMap.put("noRecordsMatch", null);
        solutionMap.put("noSetHierarchy", ArxivError.Type.INTERNAL_ERROR);
        solutionMap.put("wah", null);

        for (String code : solutionMap.keySet()) {
            when(attributes.getValue(eq("code"))).thenReturn(code);

            ArxivError.Type solution = solutionMap.get(code);
            if (solution == null) {
                assertNull(xmlParser.parseError(attributes));
            } else {
                assertEquals(solution, xmlParser.parseError(attributes).getErrorType());
            }
        }
    }



    @Test
    public void testParseDatestamp() throws Exception {
        String value = "2015-01-06";
        LocalDate answer = LocalDate.of(2015, 1, 6);

        assertEquals(answer, xmlParser.parseDatestamp(value));
    }

    @Test(expected = SAXException.class)
    public void testParseDatestamp_BadFormatShouldThrow() throws Exception {
        xmlParser.parseDatestamp("2015-01-");
    }



    @Test
    public void testParseVersionDate() throws Exception {
        String value = "Fri, 8 Feb 2013 21:00:01 GMT";
        ZonedDateTime answer = ZonedDateTime.of(2013, 2, 8, 21, 0, 1, 0, ZoneId.of("Z"));

        assertEquals(answer, xmlParser.parseVersionDate(value));
    }

    @Test(expected = SAXException.class)
    public void testParseVersionDate_BadFormatShouldThrow() throws Exception {
        xmlParser.parseVersionDate("Fri, 8 Feb 2013 21:00:01 GM");
    }



    @Test
    public void testParseCategories() throws Exception {
        String value = "quant-ph cond-mat.other hep-th math-ph math.CA math.MP nlin.SI";
        List<String> categories = xmlParser.parseCategories(value);

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
        List<String> categories = xmlParser.parseCategories("  ");
        assertTrue(categories.isEmpty());
    }



    @Test
    public void testParseCursor() throws Exception {
        when(attributes.getValue("cursor")).thenReturn("46");
        assertEquals(46, xmlParser.parseCursor(attributes).intValue());
    }

    @Test
    public void testParseCursor_BadFormatShouldReturnNull() throws Exception {
        when(attributes.getValue("cursor")).thenReturn(null);
        assertNull(xmlParser.parseCursor(attributes));

        when(attributes.getValue("cursor")).thenReturn("s");
        assertNull(xmlParser.parseCursor(attributes));
    }



    @Test
    public void testParseCompleteListSize() throws Exception {
        when(attributes.getValue("completeListSize")).thenReturn("24247247");
        assertEquals(24247247, xmlParser.parseCompleteListSize(attributes).intValue());
    }

    @Test
    public void testParseCompleteListSize_BadFormatShouldReturnNull() throws Exception {
        when(attributes.getValue("completeListSize")).thenReturn(null);
        assertNull(xmlParser.parseCompleteListSize(attributes));

        when(attributes.getValue("completeListSize")).thenReturn("s");
        assertNull(xmlParser.parseCompleteListSize(attributes));
    }
    */

}