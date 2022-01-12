/*
 */
package gov.osti.services;

import gov.osti.entity.DOECodeMetadata;
import gov.osti.entity.Developer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.jgit.util.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author ensornl
 */
public class MetadataTest {
    
    public MetadataTest() {
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
     * Test that PUBLISHED VALIDATIONS work.
     */
    @Test
    public void testValidatePublished() {
        DOECodeMetadata m = new DOECodeMetadata();
        
        // empty metadata should have numerous errors
        List<String> reasons = Metadata.validateSubmit(m);
        
        assertFalse("Validation passed?", reasons.isEmpty());
        
        String[] validations = {
            "Missing Source Project Type.",
            "Software title is required.",
            "Description is required.",
            "A License is required.",
            "At least one developer is required."
        };
        
        for ( String message : validations ) {
            assertTrue ("Missing: " + message, reasons.contains(message));
        }
        
        // fix some of these and test them
        m.setSoftwareTitle("A Testing Software Title");
        m.setProjectType(DOECodeMetadata.ProjectType.OS);
        // removes software title validation, adds OS one
        
        reasons = Metadata.validateSubmit(m);
        assertFalse("Still requiring title?", reasons.contains("Software title is required."));
        assertFalse("Still requiring project type", reasons.contains("Missing Source Project Type."));
        assertTrue ("Missing OS validation", reasons.contains("Repository URL is required for open source submissions."));
        
        // test developer issues
        Developer d = new Developer();
        List<Developer> developers = new ArrayList<>();
        developers.add(d); // empty developer
        
        m.setDevelopers(developers);
        
        reasons = Metadata.validateSubmit(m);
        assertFalse("Developer reason still there", reasons.contains("At least one developer is required."));
        assertTrue ("Missing validation on name", reasons.contains("Developer missing first name."));
        assertTrue ("Missing validation on name", reasons.contains("Developer missing last name."));
        
        // fix developer names, invalid email
        d.setFirstName("Test");
        d.setLastName("Guy");
        d.setEmail("testguy");
        
        developers.clear();
        developers.add(d);
        
        m.setDevelopers(developers);
        
        reasons = Metadata.validateSubmit(m);
        assertFalse("still missing name", reasons.contains("Developer missing first name."));
        assertFalse("still missing last name", reasons.contains("Developer missing last name."));
        assertTrue ("Missing email validation error", reasons.contains("Developer email \"testguy\" is not valid."));
        
        m.setRepositoryLink("nothing");
        reasons = Metadata.validateSubmit(m);
        
        assertFalse("Still requiring repository link", reasons.contains("Repository URL is required for open source submissions."));
        assertTrue ("Repository link should be invalid", reasons.contains("Repository URL is not a valid repository."));
        
        // test the "Other" requirement
        List<String> licenses = Arrays.asList(new String[] { "Other" });
        m.setLicenses(licenses);
        
        reasons = Metadata.validateSubmit(m);
        
        assertTrue ("Should require a proprietary URL", reasons.contains("Proprietary License URL is required."));
        
        // create something that will pass validations
        m.setProjectType(DOECodeMetadata.ProjectType.CS);
        m.setRepositoryLink("");
        m.setLandingPage("http://code.google.com/");
        d.setEmail("testguy@testing.com");
        developers.clear();
        developers.add(d);
        m.setDevelopers(developers);
        m.setProprietaryUrl("http://mylicense.com/terms.html");
        m.setDescription("This is a testing description.");
        
        reasons = Metadata.validateSubmit(m);
        
        assertTrue ("Should be no more errors: " + StringUtils.join(reasons, ", "), reasons.isEmpty());
        
    }
    
    /**
     * Test some SUBMIT validations.
     */
    @Test
    public void testValidateSubmit() {
        // test that published ones also apply here
        DOECodeMetadata m = new DOECodeMetadata();
        
        // empty metadata should have numerous errors
        List<String> reasons = Metadata.validateAnnounce(m);
        
        assertFalse("Validation passed?", reasons.isEmpty());
        
        String[] validations = {
            "Missing Source Project Type.",
            "Software title is required.",
            "Description is required.",
            "A License is required.",
            "At least one developer is required."
        };
        String[] submit_validations = {
            "Release date is required.",
            "At least one sponsoring organization is required.",
            "At least one research organization is required.",
            "Contact name is required.",
            "Contact email is required.",
            "Contact phone number is required.",
            "Contact organization is required."
        };
        
        for ( String message : validations ) {
            assertTrue ("Missing: " + message, reasons.contains(message));
        }
        
        // also check submit only validations
        for ( String message : submit_validations ) {
            assertTrue ("Missing: " + message, reasons.contains(message));
        }
    }
}
