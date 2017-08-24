package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;

/**
 * The Contributor Embeddable entity class.
 * 
 * (NOTE:  Embeddable objects don't currently inherit, thus fields are 
 * duplicated from the Agent superclass).
 * 
 * @author ensornl
 */
@Entity (name = "CONTRIBUTORS")
@JsonIgnoreProperties ( ignoreUnknown = true )
public class Contributor extends Agent {
    /**
     * Static Type of Contributor.  Based on DataCite accepted mapping values.
     */
    public enum Type {
        ContactPerson("Contact Person"),
        DataCollector("Data Collector"), 
        DataCurator("Data Curator"),  
        DataManager("Data Manager"),  
        Distributor("Distributor"), 
        Editor("Editor"), 
        HostingInstitution("Hosting Institution"),
        Producer("Producer"),
        ProjectLeader("Project Leader"),
        ProjectManager("Project Manager"), 
        ProjectMember("Project Member"),
        RegistrationAgency("Registration Agency"),
        RegistrationAuthority("Registration Authority"),
        RelatedPerson("Related Person"),
        Researcher("Researcher"),
        ResearchGroup("Research Group"),
        RightsHolder("Rights Holder"),
        Sponsor("Sponsor"),
        Supervisor("Supervisor"),
        WorkPackageLeader("Work Package Leader"),
        Other("Other");
        
        private final String label;
        
        private Type(String label) {
            this.label = label;
        }
        
        public String label() {
            return this.label;
        }
    }
    private Type contributorType;
    private List<String> affiliations;

    @Enumerated (EnumType.STRING)
    @Column (name = "CONTRIBUTOR_TYPE")
    public Type getContributorType() {
            return contributorType;
    }

    public void setContributorType(Type contributorType) {
            this.contributorType = contributorType;
    }
    
    /**
     * Get CONTRIBUTOR AFFILIATIONS.  (Evidently JPA does NOT like to do this
     * at the mapped superclass level?)
     * 
     * @return a List of affiliation names, if any
     */
    @ElementCollection
    @CollectionTable(
            name = "CONTRIBUTOR_AFFILIATIONS",
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
