/*
 */
package gov.osti.doi;

import gov.osti.entity.DOECodeMetadata;
import java.io.StringReader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test DOE CODE JSON to DataCite formatted XML output for DOI registration.
 *
 * @author ensornl
 */
public class DataCiteTest {
    // static test case for processing JSON metadata
    private static String JSON_TEST = "{\n" +
"	\"code_id\": 2551,\n" +
"	\"site_ownership_code\": \"OSTI\",\n" +
"	\"open_source\": false,\n" +
"	\"repository_link\": \"https://github.com/doecode/doecode\",\n" +
"       \"doi\":\"10.5072/dc/2017/2551\",\n" +
"	\"access_limitations\": [\"UNL\"],\n" +
"	\"developers\": [{\n" +
"		\"place\": 1,\n" +
"		\"email\": \"\",\n" +
"		\"affiliations\": [\"IIa\"],\n" +
"		\"orcid\": \"\",\n" +
"		\"first_name\": \"Shelby\",\n" +
"		\"last_name\": \"Stooksbury\",\n" +
"		\"middle_name\": \"\"\n" +
"	}, {\n" +
"		\"place\": 2,\n" +
"		\"email\": \"\",\n" +
"		\"affiliations\": [\"Oak Ridge National Laboratory\"],\n" +
"		\"orcid\": \"\",\n" +
"		\"first_name\": \"Jay Jay\",\n" +
"		\"last_name\": \"Billings\",\n" +
"		\"middle_name\": \"\"\n" +
"	}, {\n" +
"		\"place\": 3,\n" +
"		\"email\": \"knight.kathryn@gmail.com\",\n" +
"		\"affiliations\": [\"Oak Ridge National Laboratory\"],\n" +
"		\"orcid\": \"\",\n" +
"		\"first_name\": \"Katie\",\n" +
"		\"last_name\": \"Knight\",\n" +
"		\"middle_name\": \"\"\n" +
"	}, {\n" +
"		\"place\": 4,\n" +
"		\"email\": \"IanLee1521@gmail.com\",\n" +
"		\"affiliations\": [\"Lawrence Livermore National Laboratory, @LLNL\"],\n" +
"		\"orcid\": \"\",\n" +
"		\"first_name\": \"Ian\",\n" +
"		\"last_name\": \"Lee\",\n" +
"		\"middle_name\": \"\"\n" +
"	}, {\n" +
"		\"place\": 5,\n" +
"		\"email\": \"\",\n" +
"		\"affiliations\": [\"IIA\"],\n" +
"		\"orcid\": \"\",\n" +
"		\"first_name\": \"Andrew\",\n" +
"		\"last_name\": \"Smith\",\n" +
"		\"middle_name\": \"\"\n" +
"	}, {\n" +
"		\"place\": 6,\n" +
"		\"email\": \"\",\n" +
"		\"affiliations\": [\"\"],\n" +
"		\"orcid\": \"\",\n" +
"		\"first_name\": \"vowelllDOE\",\n" +
"		\"last_name\": \"(undefined)\",\n" +
"		\"middle_name\": \"\"\n" +
"	}, {\n" +
"		\"place\": 7,\n" +
"		\"email\": \"\",\n" +
"		\"affiliations\": [\"\"],\n" +
"		\"orcid\": \"\",\n" +
"		\"first_name\": \"Neal\",\n" +
"		\"last_name\": \"Ensor\",\n" +
"		\"middle_name\": \"\"\n" +
"	}, {\n" +
"		\"place\": 8,\n" +
"		\"email\": \"\",\n" +
"		\"affiliations\": [\"\"],\n" +
"		\"orcid\": \"\",\n" +
"		\"first_name\": \"nelsonjc-osti\",\n" +
"		\"last_name\": \"(undefined)\",\n" +
"		\"middle_name\": \"\"\n" +
"	}, {\n" +
"		\"place\": 9,\n" +
"		\"email\": \"sherlinec@osti.gov\",\n" +
"		\"affiliations\": [\"https://www.osti.gov/\"],\n" +
"		\"orcid\": \"\",\n" +
"		\"first_name\": \"Crystal\",\n" +
"		\"last_name\": \"Sherline\",\n" +
"		\"middle_name\": \"\"\n" +
"	}, {\n" +
"		\"place\": 10,\n" +
"		\"email\": \"\",\n" +
"		\"affiliations\": [\"\"],\n" +
"		\"orcid\": \"\",\n" +
"		\"first_name\": \"Darel\",\n" +
"		\"last_name\": \"Finkbeiner\",\n" +
"		\"middle_name\": \"\"\n" +
"	}, {\n" +
"		\"place\": 11,\n" +
"		\"email\": \"\",\n" +
"		\"affiliations\": [\"U.S. Department of Energy Office of Scientific and Technical Information\"],\n" +
"		\"orcid\": \"\",\n" +
"		\"first_name\": \"Lorrie Apple\",\n" +
"		\"last_name\": \"Johnson\",\n" +
"		\"middle_name\": \"\"\n" +
"	}, {\n" +
"		\"place\": 12,\n" +
"		\"email\": \"\",\n" +
"		\"affiliations\": [\"\"],\n" +
"		\"orcid\": \"\",\n" +
"		\"first_name\": \"Lynn\",\n" +
"		\"last_name\": \"Davis\",\n" +
"		\"middle_name\": \"\"\n" +
"	}, {\n" +
"		\"place\": 13,\n" +
"		\"email\": \"\",\n" +
"		\"affiliations\": [\"\"],\n" +
"		\"orcid\": \"\",\n" +
"		\"first_name\": \"Mike\",\n" +
"		\"last_name\": \"Hensley\",\n" +
"		\"middle_name\": \"\"\n" +
"	}, {\n" +
"		\"place\": 14,\n" +
"		\"email\": \"\",\n" +
"		\"affiliations\": [\"Information International Associates (Contractor to DOE)\"],\n" +
"		\"orcid\": \"\",\n" +
"		\"first_name\": \"Thomas\",\n" +
"		\"last_name\": \"Welsch\",\n" +
"		\"middle_name\": \"\"\n" +
"	}],\n" +
"	\"contributors\": [{\n" +
"		\"place\": 1,\n" +
"		\"email\": \"testguy@testing.com\",\n" +
"		\"affiliations\": [\"Testing Services, Inc.\"],\n" +
"		\"orcid\": \"\",\n" +
"		\"first_name\": \"Tester\",\n" +
"		\"last_name\": \"McTestyface\",\n" +
"		\"middle_name\": \"\",\n" +
"		\"contributor_type\": \"DataCurator\"\n" +
"	}],\n" +
"	\"sponsoring_organizations\": [{\n" +
"		\"place\": 1,\n" +
"		\"organization_name\": \"OSTI\",\n" +
" \"primary_award\":\"DOE-FUNDING-001\",\n" +
"		\"funding_identifiers\": [{\n" +
"			\"identifier_type\": \"AwardNumber\",\n" +
"			\"identifier_value\": \"DE-OR-1234\"\n" +
"		}, {\n" +
"			\"identifier_type\": \"BRCode\",\n" +
"			\"identifier_value\": \"BR-549\"\n" +
"		}],\n" +
"		\"DOE\": true\n" +
"	}, {\n" +
"		\"place\": 2,\n" +
"		\"organization_name\": \"University of Tennessee, Knoxville\",\n" +
            "\"primary_award\":\"UTK-FUNDME-203\"," +
"		\"funding_identifiers\": [{\n" +
"			\"identifier_type\": \"AwardNumber\",\n" +
"			\"identifier_value\": \"UTK-2342\"\n" +
"		}, {\n" +
"			\"identifier_type\": \"AwardNumber\",\n" +
"			\"identifier_value\": \"NE-2017-2342\"\n" +
"		}],\n" +
"		\"DOE\": false\n" +
"	}, {\n" +
"		\"place\": 3,\n" +
"		\"organization_name\": \"ORNL\",\n" +
            "\"primary_award\":\"ORNL-238942\"," +
"		\"funding_identifiers\": [{\n" +
"			\"identifier_type\": \"FWPNumber\",\n" +
"			\"identifier_value\": \"FWP9923\"\n" +
"		}],\n" +
"		\"DOE\": true\n" +
"	}],\n" +
"	\"contributing_organizations\": [{\n" +
"		\"place\": 1,\n" +
"		\"organization_name\": \"ORNL\",\n" +
"		\"contributor_type\": \"DataManager\",\n" +
"		\"DOE\": false\n" +
"	}],\n" +
"	\"research_organizations\": [{\n" +
"		\"place\": 1,\n" +
"		\"organization_name\": \"ICX.net Online Services\",\n" +
"		\"DOE\": false\n" +
"	}, {\n" +
"		\"place\": 2,\n" +
"		\"organization_name\": \"Tester Services, Inc.\",\n" +
"		\"DOE\": false\n" +
"	}],\n" +
"	\"related_identifiers\": [{\n" +
"		\"identifier_type\": \"DOI\",\n" +
"		\"identifier_value\": \"10.5072/OSTI/2017/1\",\n" +
"		\"relation_type\": \"IsSourceOf\"\n" +
"	}],\n" +
"	\"release_date\": \"2016-02-03\",\n" +
"	\"software_title\": \"doecode/doecode\",\n" +
"	\"acronym\": \"doecode\",\n" +
            "\"keywords\":\"linux;repository information;reporting requirements\"," +
"	\"description\": \"Main repo for managing the new DOE CODE site from the DOE Office of Scientific and Technical Information (OSTI)\",\n" +
"	\"licenses\": [],\n" +
"	\"workflow_status\": \"Saved\"\n" +
"}";
    
    public DataCiteTest() {
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
     * Test of writeMetadata method, of class DataCite.
     * @throws Exception on unexpected errors
     */
    @Test
    public void testWriteMetadata() throws Exception {
        DOECodeMetadata md = DOECodeMetadata.parseJson(new StringReader(JSON_TEST));
        
        // test JSON interpretation was correct
        assertEquals("Code ID wrong", Long.valueOf("2551"), md.getCodeId());
        
        assertEquals("Wrong number of developers", 14, md.getDevelopers().size());
        assertEquals("Wrong number of contributors", 1, md.getContributors().size());
        assertEquals("Wrong sponsor count", 3, md.getSponsoringOrganizations().size());
        
        // peek at the XML output to make sure certain tags are present
        // DOI is assumed to be set already by the UI/front-end
        String datacite_xml = DataCite.writeMetadata(md);
        
        assertTrue ("DOI identifier missing", 
                datacite_xml.contains("<identifier identifierType=\"DOI\">10.5072/dc/2017/2551</identifier>"));
        assertTrue ("Resource type missing", 
                datacite_xml.contains("<resourceType resourceTypeGeneral=\"Software\">doecode/doecode</resourceType>"));
        assertTrue ("First developer missing",
                datacite_xml.contains(
                        "<creators><creator><creatorName>Stooksbury, Shelby</creatorName><givenName>Shelby</givenName><familyName>Stooksbury</familyName><affiliation>IIa</affiliation></creator>"));
        assertTrue ("Title is missing",
                datacite_xml.contains(
                    "<titles>" +
                    "<title>doecode/doecode</title>" +
                    "</titles>"));
        assertTrue ("Publication year is missing",
                datacite_xml.contains(
                    "<publicationYear>2016</publicationYear>"
                ));
        
        // assert those FUNDING REFERENCES are present
        assertTrue  ("Funding references wrong or missing",
                datacite_xml.contains(
                "<fundingReferences><fundingReference><funderName>OSTI</funderName><awardNumber>DOE-FUNDING-001</awardNumber></fundingReference><fundingReference><funderName>University of Tennessee, Knoxville</funderName><awardNumber>UTK-FUNDME-203</awardNumber></fundingReference><fundingReference><funderName>ORNL</funderName><awardNumber>ORNL-238942</awardNumber></fundingReference></fundingReferences>"));
    }
    
}
