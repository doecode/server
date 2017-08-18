/*
 */
package gov.osti.connectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Simple value tests for SourceForge API reader.
 * 
 * @author ensornl
 */
public class SourceForgeTest {
    
    public SourceForgeTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getProjectNameFromUrl method, of class SourceForge.
     */
    @Test
    public void testGetProjectNameFromUrl() {
        String[] badvalues = {
            "/projects/url",
            "sourceforge.com/projects/me",
            "https://sourceforge/projects/nope",
            "",
            null
        };
        
        for ( String value : badvalues ) {
            assertNull ("Found acceptable: " + value, SourceForge.getProjectNameFromUrl(value));
        }
        
        assertEquals("Matcher failed", "doecode", SourceForge.getProjectNameFromUrl("https://sourceforge.net/projects/doecode"));
        assertEquals("Matcher failed", "doecode/", SourceForge.getProjectNameFromUrl("http://sourceforge.net/projects/doecode/"));
    }
    
}
