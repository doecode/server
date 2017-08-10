/*
 */
package gov.osti.services;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test utility and functional methods in UserServices.
 * 
 * @author ensornl
 */
public class UserServicesTest {
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
    
    @Test
    public void testPasswordValidation() {
        String[] bad_passwords = { null, "", "12345678", "justlowercase",
            "$1tS",
            "JUSTUPPERCASE", "number2BUTnospecial", "!@#&%*$@#", 
            "12345$28923", "just$1lower", "JUST$5UPPER",
            "email@company.com", "#1email@company.comHIDDEN",
            "EMAIL@COMPANY.COM$4me!", "justEMAIL@company.COM2hide"
        };
        String[] good_passwords = {
            "Just$4kicks!", "Some4!funTIMES", "Not#2SHORTguy"
        };
        
        for ( String password : bad_passwords ) {
            assertFalse ("Password : " + password + " accepted!", 
                    UserServices.validatePassword("email@company.com", password));
        }
        
        for ( String password : good_passwords ) {
            assertTrue  ("Password " + password + " was NOT accepted!",
                    UserServices.validatePassword("email@company.com", password));
        }
    }
}
