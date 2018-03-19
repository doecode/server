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
     * Test extraction of the project name from various SOURCEFORGE repository URLs.
     */
    @Test
    public void testGetProjectNameFromUrl() {
        // these aren't recognized
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
        
        assertEquals("arpa project name wrong", "arpa_project_x", SourceForge.getProjectNameFromUrl("https://svn.code.sf.net/p/arpa_project_x/code/trunk"));
        
        assertEquals("wrong niceproject", "niceproject", SourceForge.getProjectNameFromUrl("http://svn.code.sf.net/p/niceproject/code"));
        assertEquals("Not ats", "ats-automatedtestingsystem", SourceForge.getProjectNameFromUrl("https://svn.code.sf.net/p/ats-automatedtestingsystem/code/"));
        
        assertEquals("Matcher failed", "doecode", SourceForge.getProjectNameFromUrl("https://sourceforge.net/projects/doecode"));
        assertEquals("Matcher failed", "doecode", SourceForge.getProjectNameFromUrl("http://sourceforge.net/projects/doecode/"));
    }
    
}
