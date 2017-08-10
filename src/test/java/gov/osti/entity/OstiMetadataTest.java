/*
 */
package gov.osti.entity;

import com.neovisionaries.i18n.CountryCode;
import java.util.List;
import java.util.regex.Pattern;
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
public class OstiMetadataTest {
    
    public OstiMetadataTest() {
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
    
    private List<CountryCode> findByName(String name) {
        return CountryCode.findByName(Pattern.compile(name, Pattern.CASE_INSENSITIVE));
    }
    
    @Test
    public void testCountryCodes() {
        List<CountryCode> countries = CountryCode.findByName("United States");
        
        assertFalse ("no countries found", countries.isEmpty());
        assertTrue  ("US wasn't in there", countries.contains(CountryCode.US));
        
        assertTrue  ("Case didn't work?", findByName("united states").contains(CountryCode.US));
        assertFalse ("Shouldn't find by US.", findByName("US").contains(CountryCode.US));
    }
}
