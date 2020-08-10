/*
 */
package gov.osti.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import org.apache.commons.lang3.StringUtils;

/**
 * A non-Person sponsoring entity.
 * 
 * @author ensornl
 */
@Entity (name = "SPONSORING_ORGANIZATIONS")
public class SponsoringOrganization extends Organization {
    @JacksonXmlElementWrapper (localName = "fundingIdentifiers")
    @JacksonXmlProperty (localName = "fundingIdentifier")
    private List<FundingIdentifier> fundingIdentifiers = new ArrayList<>();
    private String primaryAward;
    
    /**
     * Get the PRIMARY award number.
     * 
     * @return the PRIMARY AWARD NUMBER (required)
     */
    @Column (length = 500, name = "PRIMARY_AWARD")
    public String getPrimaryAward() {
        return primaryAward;
    }
    
    /**
     * Set the PRIMARY AWARD number.
     * 
     * @param award the PRIMARY AWARD NUMBER
     */
    public void setPrimaryAward(String award) {
        primaryAward = award;
    }
    
    @ElementCollection
    @CollectionTable(
            name = "FUNDING_IDENTIFIERS",
            joinColumns=@JoinColumn(name="ORG_ID")
    )
    public List<FundingIdentifier> getFundingIdentifiers() {
        return fundingIdentifiers;
    }
    
    public void setFundingIdentifiers(List<FundingIdentifier> list) {
        this.fundingIdentifiers = CleanFundersList(list);
    }

    protected List<FundingIdentifier> CleanFundersList(List<FundingIdentifier> list) {
        if (list != null) {
            // remove empty data
            int cnt = list.size();
            for (int i = cnt - 1; i >= 0; i--) {
                FundingIdentifier f = list.get(i);
                if (f == null) {
                    list.remove(i);
                }
                else {
                    String s = f.getIdentifierValue();
                    if (StringUtils.isBlank(s))
                        list.remove(i);
                }
            }
        }

        return list;
    }
}
