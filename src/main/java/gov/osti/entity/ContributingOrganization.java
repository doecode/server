/*
 */
package gov.osti.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * Contributing Organization information.
 * @author ensornl
 */
@Entity (name = "CONTRIBUTING_ORGANIZATIONS")
public class ContributingOrganization extends Organization {
    /**
     * Static Type of Contributor.  Based on DataCite accepted mapping values for organizations.
     */
    public enum Type {
        Distributor("Distributor"), 
        HostingInstitution("Hosting Institution"),
        RegistrationAgency("Registration Agency"),
        RegistrationAuthority("Registration Authority"),
        ResearchGroup("Research Group"),
        ContactPerson("Contact Person"),
        DataCollector("Data Collector"), 
        DataCurator("Data Curator"),  
        DataManager("Data Manager"),  
        Producer("Producer"),
        RightsHolder("Rights Holder"),
        Sponsor("Sponsor"),
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
    
    // each have a Type of contribution
    private Type contributorType;
    
    /**
     * @return the type
     */
    @Enumerated (EnumType.STRING)
    @Column (name="CONTRIBUTOR_TYPE")
    public Type getContributorType() {
        return contributorType;
    }

    /**
     * @param type the type to set
     */
    public void setContributorType(Type type) {
        this.contributorType = type;
    }
    
    
}
