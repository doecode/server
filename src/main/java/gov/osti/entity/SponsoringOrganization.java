/*
 */
package gov.osti.entity;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;

/**
 * A non-Person sponsoring entity.
 * 
 * @author ensornl
 */
@Entity (name = "SPONSORING_ORGANIZATIONS")
public class SponsoringOrganization extends Organization {
    private List<FundingIdentifier> fundingIdentifiers = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(
            name = "FUNDING_IDENTIFIERS",
            joinColumns=@JoinColumn(name="ORG_ID")
    )
    public List<FundingIdentifier> getFundingIdentifiers() {
        return fundingIdentifiers;
    }
    
    public void setFundingIdentifiers(List<FundingIdentifier> list) {
        this.fundingIdentifiers = list;
    }
}
