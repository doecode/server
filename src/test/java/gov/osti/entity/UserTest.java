/*
 */
package gov.osti.entity;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.Set;
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
    
    @Test
    public void testDefaults() {
        User user = new User();
        
        assertFalse ("Blank user should not be verified", user.isVerified());
        assertFalse ("Blank user should not be active", user.isActive());
        assertTrue  ("Empty user should be expired", user.isPasswordExpired());
        assertEquals("Should be no strikes", 0, user.getFailedCount());
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
