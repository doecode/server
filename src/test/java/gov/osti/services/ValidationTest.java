/*
 */
package gov.osti.services;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ensornl
 */
public class ValidationTest {
    
    public ValidationTest() {
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
     * Test of isValidEmail method, of class Validation.
     */
    @Test
    public void testIsValidEmail() {
        String[] valid = {"me@anotherspot.com",
            "someguy@someplace.com",
            "TeeeeeeeeNTeeeeeeeeNTeeeeeeeeNTeeeeeeeeNTeeeeeeeeNTeeeeeeeeNatuu@test.gov",
            "a.ten@example.com",
            "--ten@example.com",
            "em@a-exampe.apple.com",
            "em@example.-apple.com",
            "eapp+em@example.-apple.com",
            "doecode---@9-ostie.app.-le.gov"};
        String[] invalid = { "email@-example.com",
            "te??n@example.com",
            "em@-exampe.app-le.com",
            "ten.@example.com",
            "a..ten@example.com",
            ".ten@example.com",
            "TeeeeeeeeNTeeeeeeeeNTeeeeeeeeNTeeeeeeeeNTeeeeeeeeNTeeeeeeeeNaetuu@test.gov",
            "someguy",
            "@domain.com",
            "nope@",
            "notme#noplace.com",
            "example@-aoe.com"};
        
        for ( String address : valid ) {
            assertTrue  ("Should be valid: " + address, Validation.isValidEmail(address));
        }
        
        for ( String address : invalid ) {
            assertFalse("Should have rejected: " + address, Validation.isValidEmail(address));
        }
    }

    /**
     * Test for specific failing Boolean conversions in API responses.
     * 
     * @throws IOException on unexpected errors
     */
    @Test
    public void testApiResponse() throws IOException {
        ApiResponse response = Validation.mapper.readValue("{}", ApiResponse.class);
        
        assertFalse ("Empty JSON should be invalid", response.isValid());
    }
    
    /**
     * Test of isValidUrl method, of class Validation.
     */
    @Test
    public void testIsValidUrl() {
        // PATTERN MATCH against URL-type Strings
        String[] valid = {
            "http://google.com",
            "https://www.osti.gov/someplace/page.html",
            "http://kumquat.org/"
        };
        // only accepts http:// or https:// addresses by design
        String[] invalid = {
            "", 
            "missing",
            "something.org",
            "https://",
            "http://",
            "ftp://somesite.net/bogus.file"
        };
        
        for ( String url : valid ) {
            assertTrue  ("URL matcher failed for: " + url, Validation.isValidUrl(url));
        }
        
        for ( String url : invalid ) {
            assertFalse ("URL passed: " + url, Validation.isValidUrl(url));
        }
    }

    /**
     * Test of isValidPhoneNumber method, of class Validation.
     */
    @Test
    public void testIsValidPhoneNumber() {
        String[] valid = { "865-555-5555", "8655555555", "(865)457-2222", "(865)4572222",
            "865-457-2222", "865-4572222"
        };
        String[] invalid = { "", "5", "123-4567", "11-12345" };
        
        for ( String pn : valid ) {
            assertTrue  ("Should be ok: " + pn, Validation.isValidPhoneNumber(pn));
        }
        
        for ( String pn : invalid ) {
            assertFalse ("Accepted: " + pn, Validation.isValidPhoneNumber(pn));
        }
    }

    /**
     * Test of isValidAwardNumber method, of class Validation.
     */
    @Test
    public void testIsValidAwardNumber() {
        // uses non-static data for testing?
    }

    /**
     * Test of isValidRepositoryLink method, of class Validation.
     */
    @Test
    public void testIsValidRepositoryLink() {
        // validates external non-static content
    }

    @Test
    public void testIsValidDoi() {
        // some bad non-DOI patterns
        String[] invalid = { "0.2343/232343/2323", "1.2234/is/not/a/DOI",
            "http://notadoi.com/", ""
        };
        // valid DOI patterns
        String[] valid = { "10.5072/238492", "10.5072/software/version01/1283",
            "10.3334/just/sample/data010", "http://doi.org/10.5072/348293",
            "https://doi.org/10.5072/999/23423.journal.2342"
        };
        
        for ( String doi : invalid ) {
            assertFalse ("Shouldn't accept: " + doi, Validation.isValidDoi(doi));
        }
        
        for ( String doi : valid ) {
            assertTrue  ("DOI " + doi + " should pass.", Validation.isValidDoi(doi));
        }
    }

    @Test
    public void testIsValidORCID() {
        // some bad non-DOI patterns
        String[] invalid = { "12345678901234567", "000000021825009X",
            "http://notaorcid.com/", ""
        };
        // valid DOI patterns
        String[] valid = { "0000000218250097", "0000-0003-3348-0736",
            "HTTPS://orcid.org/0000-0001-8811-2688", "  00000002-5667291x  "
        };
        
        for ( String orcid : invalid ) {
            assertFalse ("Shouldn't accept: " + orcid, Validation.isValidORCID(orcid));
        }
        
        for ( String orcid : valid ) {
            assertTrue  ("ORCID " + orcid + " should pass.", Validation.isValidORCID(orcid));
        }
    }
    
}
