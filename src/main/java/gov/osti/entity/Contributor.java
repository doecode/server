package gov.osti.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * The Contributor Embeddable entity class.
 * 
 * (NOTE:  Embeddable objects don't currently inherit, thus fields are 
 * duplicated from the Agent superclass).
 * 
 * @author ensornl
 */
@Entity (name = "CONTRIBUTORS")
//public class Contributor implements Serializable {
public class Contributor extends Agent {
    /**
     * Static Type of Contributor.  Based on DataCite accepted mapping values.
     */
    public enum Type {
        ContactPerson,
        DataCollector, 
        DataCurator,  
        DataManager,  
        Distributor, 
        Editor, 
        HostingInstitution,
        Producer,
        ProjectLeader,
        ProjectManager, 
        ProjectMember,
        RegistrationAgency,
        RegistrationAuthority,
        RelatedPerson,
        Researcher,
        ResearchGroup,
        RightsHolder,
        Sponsor,
        Supervisor,
        WorkPackageLeader,
        Other 
    }
    private Type contributorType;

    @Enumerated (EnumType.STRING)
    @Column (name = "CONTRIBUTOR_TYPE")
    public Type getContributorType() {
            return contributorType;
    }

    public void setContributorType(Type contributorType) {
            this.contributorType = contributorType;
    }
}
