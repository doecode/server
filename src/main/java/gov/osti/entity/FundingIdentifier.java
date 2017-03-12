
package gov.osti.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * Represent a FUNDING IDENTIFIER one-to-many child of SponsoringOrganization.
 * 
 * @author nensor
 */
@Embeddable
public class FundingIdentifier implements Serializable {
    public enum Type {
        AwardNumber,
        BRCode,
        FWPNumber;
        
        public String getName() {
            return this.name();
        }
    }
    // attributes
    private Type identifierType;
    private String identifierValue;

    /**
     * @return the type
     */
    @Enumerated (EnumType.STRING)
    public Type getIdentifierType() {
        return identifierType;
    }

    /**
     * @param type the type to set
     */
    public void setIdentifierType(Type type) {
        this.identifierType = type;
    }

    /**
     * @return the value
     */
    @Column (name="IDENTIFIER_VALUE", length = 1000)
    public String getIdentifierValue() {
        return identifierValue;
    }

    /**
     * @param value the value to set
     */
    public void setIdentifierValue(String value) {
        this.identifierValue = value;
    }
    
    
}
