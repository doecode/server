/*
 */
package gov.osti.connectors;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gov.osti.entity.ContributingOrganization;
import gov.osti.entity.Contributor;
import gov.osti.entity.DOECodeMetadata;
import gov.osti.entity.Developer;
import gov.osti.entity.FundingIdentifier;
import gov.osti.entity.RelatedIdentifier;
import gov.osti.entity.ResearchOrganization;
import gov.osti.entity.SponsoringOrganization;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ensornl
 */
public class MetadataYamlTest {
    // the base execution directory for Junit tests
    private static final String basedir = System.getProperty("basedir");
    // logger
    private static final Logger log = LoggerFactory.getLogger(MetadataYamlTest.class);
    
    public MetadataYamlTest() {
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
     * Obtain a filesystem path for testing resource files.
     * 
     * @param filename the relative filename to map
     * @return a filesystem path to that file
     */
    private static final String getPathFor(String filename) {
        return basedir + File.separator + "src" + File.separator + "test" + File.separator + filename;
    }
    
    /**
     * Test YAML-to-DOECodeMetadata translation.
     * 
     * @throws Exception on unexpected errors
     */
    @Test
    public void testYamlToMetadata() throws Exception {
        final ObjectMapper mapper = 
                new ObjectMapper(new YAMLFactory())
                .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
                .setSerializationInclusion(Include.NON_NULL)
                .setTimeZone(TimeZone.getDefault());
        
        DOECodeMetadata metadata = mapper.readValue(new FileReader(getPathFor("metadata.yml")), DOECodeMetadata.class);
        
        assertNotNull("Metadata parsing failed!", metadata);
        
        assertEquals("Repo URL wrong", "https://github.com/doecode/dev-test-repo", metadata.getRepositoryLink());
        
        List<Developer> developers = metadata.getDevelopers();
        assertEquals("Wrong number of developers", 4, developers.size());
        
        for ( Developer d : developers ) {
            if ("Jay Jay".equals(d.getFirstName())) {
                assertEquals("Last name wrong", "Billings", d.getLastName());
                assertNotNull("Affiliations null", d.getAffiliations());
                assertEquals("Count is wrong", 1, d.getAffiliations().size());
                assertFalse ("Missing affiliations", d.getAffiliations().isEmpty());
                assertTrue  ("Affiliations wrong", d.getAffiliations().contains("Oak Ridge National Laboratory"));
            } else if ("Knight".equals(d.getLastName())) {
                assertEquals("First name wrong", "Katie", d.getFirstName());
                assertEquals("Email wrong", "knight.kathryn@gmail.com", d.getEmail());
                assertNotNull("Missing affiliations", d.getAffiliations());
                assertEquals("Count wrong", 1, d.getAffiliations().size());
                assertTrue  ("Affiliations wrong", d.getAffiliations().contains("Oak Ridge National Laboratory"));
            } else if ("Ensor".equals(d.getLastName())) {
                assertEquals("First name wrong", "Neal", d.getFirstName());
                assertEquals("email wrong", "ensorn@osti.gov", d.getEmail());
            } else if ("Welsch".equals(d.getLastName())) {
                assertEquals("First name wrong", "Thomas", d.getFirstName());
                assertNotNull("Affiliations missing", d.getAffiliations());
                assertEquals("Count wrong", 1, d.getAffiliations().size());
                assertTrue  ("Affiliation wrong", d.getAffiliations().contains("Information International Associates (Contractor to DOE)"));
                assertEquals("email wrong", "welscht@osti.gov", d.getEmail());
            } else {
                fail ("Unknown developer found: " + d.getLastName());
            }
        }
        assertEquals("title is wrong", "Sample development and testing repository for DOE CODE", metadata.getSoftwareTitle());
        assertEquals("Acronym wrong", "dev-test-repo", metadata.getAcronym());
        assertEquals("DOI wrong", "10.5072/23892", metadata.getDoi());
        assertEquals("Description wrong", "An example testing repository for submissions of YAML and other associated testing projects related to the development of DOE CODE.\n",
                metadata.getDescription());
        
        List<String> licenses = metadata.getLicenses();
        assertEquals("Should be 3 licenses", 3, licenses.size());
        
        for ( String l : licenses ) {
            log.info("License: " + l);
        }
        
        assertTrue  ("Missing GNU", licenses.contains("GNU v.3"));
        assertTrue  ("Missing apache", licenses.contains("Apache 2.0"));
        assertTrue  ("Missing CC license", licenses.contains("Creative Commons License"));
        
        List<RelatedIdentifier> identifiers = metadata.getRelatedIdentifiers();
        assertEquals("There should be 2 identifiers", 2, identifiers.size());
        
        RelatedIdentifier identifier = identifiers.get(0);
        assertEquals ("ID#1 type wrong", RelatedIdentifier.Type.URL, identifier.getIdentifierType());
        assertEquals ("ID#1 value wrong", "http://github.com/doecode/doecode", identifier.getIdentifierValue());
        assertEquals ("ID#1 relation wrong", RelatedIdentifier.RelationType.IsPreviousVersionOf, identifier.getRelationType());
        
        identifier = identifiers.get(1);
        
        assertEquals("ID#2 type wrong", RelatedIdentifier.Type.DOI, identifier.getIdentifierType());
        assertEquals("ID#2 value wrong", "10.5072/doecode2017/dev-test-repo/2", identifier.getIdentifierValue());
        assertEquals("ID#2 relation wrong", RelatedIdentifier.RelationType.Cites, identifier.getRelationType());
        
        assertEquals("Keywords wrong", "software, doecode, DOE CODE, hosting repositories", metadata.getKeywords());
        assertEquals("disclaimers wrong", "open source", metadata.getDisclaimers());
        assertEquals("recipient name wrong", "Neal Ensor", metadata.getRecipientName());
        assertEquals("recipient email wrong", "ensorn@osti.gov", metadata.getRecipientEmail());
        assertEquals("recipient phone wrong", "865-576-1295", metadata.getRecipientPhone());
        assertEquals("recipient org wrong", "DOE OSTI", metadata.getRecipientOrg());
        assertEquals("Accession number wrong", "384983", metadata.getSiteAccessionNumber());
        assertEquals("other requirements wrong", "none", metadata.getOtherSpecialRequirements());
        
        // check sponsoring orgs
        List<SponsoringOrganization> sponsors = metadata.getSponsoringOrganizations();
        
        assertEquals("There should be 2", 2, sponsors.size());
        
        SponsoringOrganization sponsor = sponsors.get(0);
        assertEquals("Name is wrong", "DOE OSTI", sponsor.getOrganizationName());
        assertTrue  ("Should be DOE", sponsor.isDOE());
        assertEquals("Primary award number wrong", "DE-654321", sponsor.getPrimaryAward());
        List<FundingIdentifier> funding_identifiers = sponsor.getFundingIdentifiers();
        assertEquals("There should be 2 award numbers", 2, funding_identifiers.size());
        assertEquals("First award wrong", "DE-865234", funding_identifiers.get(0).getIdentifierValue());
        assertEquals("First type wrong", FundingIdentifier.Type.AwardNumber, funding_identifiers.get(0).getIdentifierType());
        assertEquals("Second award wrong", "DE-8293", funding_identifiers.get(1).getIdentifierValue());
        assertEquals("Second type wrong", FundingIdentifier.Type.AwardNumber, funding_identifiers.get(1).getIdentifierType());
        
        sponsor = sponsors.get(1);
        assertEquals("Name is wrong", "University of Miami, FL", sponsor.getOrganizationName());
        assertFalse ("Should NOT be DOE", sponsor.isDOE());
        assertEquals("Primary award number wrong", "UFL-1234", sponsor.getPrimaryAward());
        funding_identifiers = sponsor.getFundingIdentifiers();
        assertEquals("There should be 3 awards", 3, funding_identifiers.size());
        assertEquals("award number 1 wrong", "UWIN-234", funding_identifiers.get(0).getIdentifierValue());
        assertEquals("type1 wrong", FundingIdentifier.Type.AwardNumber, funding_identifiers.get(0).getIdentifierType());
        assertEquals("award number 2 wrong", "UWIN-888", funding_identifiers.get(1).getIdentifierValue());
        assertEquals("type2 wrong", FundingIdentifier.Type.AwardNumber, funding_identifiers.get(1).getIdentifierType());
        assertEquals("award number 3 wrong", "UFL-11", funding_identifiers.get(2).getIdentifierValue());
        assertEquals("type3 wrong", FundingIdentifier.Type.AwardNumber, funding_identifiers.get(2).getIdentifierType());
        
        List<Contributor> contributors = metadata.getContributors();
        
        assertEquals("There should be 1 contributor", 1, contributors.size());
        Contributor contributor = contributors.get(0);
        assertEquals("Contributor name wrong", "Bob", contributor.getFirstName());
        assertEquals("Contributor name wrong", "McTester", contributor.getLastName());
        assertNotNull("Missing affiliation", contributor.getAffiliations());
        assertEquals("affiliation count wrong", 1, contributor.getAffiliations().size());
        assertTrue  ("Affiliation wrong", contributor.getAffiliations().contains("Java Testing Services, Inc."));
        assertEquals("Contributor type wrong", Contributor.Type.DataCurator, contributor.getContributorType());
        assertEquals("Email wrong", "mctester@testingservices.com", contributor.getEmail());
        
        List<ContributingOrganization> contribs = metadata.getContributingOrganizations();
        assertEquals("There should be 1 org", 1, contribs.size());
        ContributingOrganization corg = contribs.get(0);
        assertEquals("Org name wrong", "University of Wisconsin", corg.getOrganizationName());
        assertEquals("Type wrong", Contributor.Type.DataCurator, corg.getContributorType());
        
        List<ResearchOrganization> research_orgs = metadata.getResearchOrganizations();
        assertEquals("There should be 2 research orgs", 2, research_orgs.size());
        ResearchOrganization reorg = research_orgs.get(0);
        assertEquals("Org name wrong", "OSTI", reorg.getOrganizationName());
        assertTrue  ("Should be DOE", reorg.isDOE());
        reorg = research_orgs.get(1);
        assertEquals("Org name is wrong", "ORNL", reorg.getOrganizationName());
        assertTrue  ("Should be DOE", reorg.isDOE());
        
        Date issue_date = metadata.getReleaseDate();
        
        assertNotNull("Date is missing", issue_date);
        assertEquals ("Date is wrong", "06/02/1999", 
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
                        .withZone(ZoneId.of("UTC"))
                        .format(issue_date.toInstant()));
        
        log.info("Writing back out:");
        log.info(HttpUtil.writeMetadataYaml(metadata));
    }
    
}
