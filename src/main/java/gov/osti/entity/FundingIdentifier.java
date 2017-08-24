
package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties ( ignoreUnknown = true )
public class FundingIdentifier implements Serializable {
    public enum Type {
        AwardNumber("Award Number"),
        BRCode("BR Code"),
        FWPNumber("FWP Number");
        
        private final String label;
        
        private Type(String label) {
            this.label = label;
        }
        
        public String label() {
            return this.label;
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
    @Column (name = "IDENTIFIER_TYPE")
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
