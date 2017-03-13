
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
     * The TYPE of this identifier.
     * 
     * @return the type
     */
    @Enumerated (EnumType.STRING)
    public Type getIdentifierType() {
        return identifierType;
    }

    /**
     * Set a TYPE for this identifier
     * @param type the type to set
     */
    public void setIdentifierType(Type type) {
        this.identifierType = type;
    }

    /**
     * Get the VALUE of the identifier
     * @return the value
     */
    @Column (name="IDENTIFIER_VALUE", length = 1000)
    public String getIdentifierValue() {
        return identifierValue;
    }

    /**
     * Set the VALUE of the identifier
     * @param value the value to set
     */
    public void setIdentifierValue(String value) {
        this.identifierValue = value;
    }
    
    
}
