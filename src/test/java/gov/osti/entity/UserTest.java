/*
 */
package gov.osti.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
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
public class UserTest {
    
    public UserTest() {
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
     * Test of isPasswordExpired method, of class User.
     */
    @Test
    public void testIsPasswordExpired() {
        User user = new User();
        
        // default
        assertTrue  ("Empty should be expired", user.isPasswordExpired());
        
        // set a date to now
        user.setDatePasswordChanged();
        assertFalse ("Now should not be expired", user.isPasswordExpired());
        
        // move the date back at least PASSWORD AGE
        user.setDatePasswordChanged(Date.from(Instant.now().minus(Duration.ofDays(User.PASSWORD_DATE_EXPIRATION_IN_DAYS))));
        assertTrue  ("Date of " + user.getDatePasswordChanged() + " not expired", user.isPasswordExpired());
        
    }
    
}
