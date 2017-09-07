package gov.osti.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;

/**
 * The Developer Agent mapping.
 * 
 * @author ensornl
 */
@Entity (name = "DEVELOPERS")
public class Developer extends Agent {

    @JacksonXmlElementWrapper (localName = "affiliations")
    @JacksonXmlProperty (localName = "affiliation")
    private List<String> affiliations;
    
    public Developer() {
        super();
    }
    
    /**
     * Get a List of DEVELOPER AFFILIATIONS.  (JPA doesn't appear to support
     * this at the mapped superclass level, thus separate tables for
     * CONTRIBUTOR and DEVELOPER).
     * 
     * @return a List of developer affiliation names, if any
     */
    @ElementCollection
    @CollectionTable(
            name = "DEVELOPER_AFFILIATIONS",
            joinColumns=@JoinColumn(name="AGENT_ID")
    )
    @Column (name = "AFFILIATION")
    public List<String> getAffiliations() {
            return affiliations;
    }
    public void setAffiliations(List<String> affiliations) {
            this.affiliations = affiliations;
    }
}
