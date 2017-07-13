/*
 */
package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Superclass for Organization type relations.
 * 
 * Extended/implemented by ContributingOrganization, SponsoringOrganization,
 * and ResearchOrganization.
 * 
 * @author ensornl
 */
@MappedSuperclass
@JsonIgnoreProperties ( ignoreUnknown = true )
public class Organization implements Serializable {
    // primary Key
    private Long orgId = 0L;
    // attributes
    private String organizationName;
    @JsonProperty (value="DOE")
    private boolean doe = false;
    
    @Id
    @GeneratedValue (strategy = GenerationType.AUTO)
    @Column (name="ORG_ID")
    @JsonIgnore
    public Long getOrgId() {
        return this.orgId;
    }
    
    public void setOrgId(Long id) {
        this.orgId = id;
    }
    
    /**
     * @return the organizationName
     */
    @Column (length = 1000, name = "ORGANIZATION_NAME")
    public String getOrganizationName() {
        return organizationName;
    }

    /**
     * @param organizationName the organizationName to set
     */
    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }
    
    /**
     * Whether or not this ORGANIZATION is DOE-based.
     * 
     * @return true if this is a DOE organization, false if not
     */
    @Column (name="DOE")
    public boolean isDOE() {
        return doe;
    }
    
    /**
     * Set whether or not this is a DOE-based ORGANIZATION
     * @param isDOE whether or not this ORGANIZATION is DOE
     */
    public void setIsDOE(boolean isDOE) {
        this.doe = isDOE;
    }
    
}
