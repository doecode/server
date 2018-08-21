/*
 */
package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * A Generic Identifier for metadata; includes Related Identifiers, BR Codes, etc.
 * 
 * @author ensornl
 */
@Embeddable
@JsonIgnoreProperties ( ignoreUnknown = true )
public class RelatedIdentifier implements Serializable {
    /**
     * Enumeration of valid Types for an Identifier.
     */
    public enum Type implements Serializable {
        DOI("DOI"),
        URL("URL");
        
        private final String label;
        
        private Type(String label) {
            this.label =label;
        }
        
        public String label() {
            return this.label;
        }
    }
    /**
     * Enumeration of valid Relationship Types for Identifier.
     */
    public enum RelationType implements Serializable {
        IsCitedBy("Is Cited By"),
        Cites("Cites"), 
        IsSupplementTo("Is Supplement To"),
        IsSupplementedBy("Is Supplemented By"),
        IsContinuedBy("Is Continued By"), 
        Continues("Continues"), 
        HasMetadata("Has Metadata"), 
        IsMetadataFor("Is Metadata For"), 
        IsNewVersionOf("Is New Version Of"), 
        IsPreviousVersionOf("Is Previous Version Of"), 
        IsPartOf("Is Part Of"), 
        HasPart("Has Part"), 
        IsReferencedBy("Is Referenced By"),
        References("References"), 
        IsDocumentedBy("Is Documented By"), 
        Documents("Documents"), 
        IsCompiledBy("Is Compiled By"), 
        Compiles("Compiles"), 
        IsVariantFormOf("Is Variant Form Of"), 
        IsOriginalFormOf("Is Original Form Of"), 
        IsIdenticalTo("Is Identical To"), 
        IsReviewedBy("Is Reviewed By"), 
        Reviews("Reviews"), 
        IsDerivedFrom("Is Derived From"), 
        IsSourceOf("Is Source Of"),
        IsDescribedBy("Is Described By"),
        Describes("Describes"),
        HasVersion("Has Version"),
        IsVersionOf("Is Version Of"),
        IsRequiredBy("Is Required By"),
        Requires("Requires");
        
        private final String label;
        
        private RelationType(String label) {
            this.label = label;
        }
        
        public String label() {
            return this.label;
        }
    }

    // the specific Type of the Identifier
    private Type identifierType;
    // optional String description of this Identifier
    private String description;
    // the value of the Identifier
    private String identifierValue;
    // the Relationship Type (if any) for this Identifier
    private RelationType relationType;

    public RelatedIdentifier() {
        
    }
    
    public RelatedIdentifier(Type idType, String value, RelationType relType) {
        this.identifierType = idType;
        this.identifierValue = value;
        this.relationType = relType;
    }
    
    
    /**
     * @return the type
     */
    @Enumerated (EnumType.STRING)
    @Column (name = "IDENTIFIER_TYPE")
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
     * Get the Description for this Identifier
     * @return the Description
     */
    @Column (length = 500, name="DESCRIPTION")
    public String getDescription() {
        return this.description;
    }
    
    /**
     * Add a description (optional) for this Identifier
     * @param d the Description to use
     */
    public void setDescription(String d) {
        this.description = d;
    }

    /**
     * @return the value
     */
    @Column (name = "IDENTIFIER_VALUE", length = 1000)
    public String getIdentifierValue() {
        return identifierValue;
    }

    /**
     * @param value the value to set
     */
    public void setIdentifierValue(String value) {
        this.identifierValue = value;
    }

    /**
     * @return the relation
     */
    @Enumerated (EnumType.STRING)
    @Column (name = "RELATION_TYPE")
    public RelationType getRelationType() {
        return relationType;
    }

    /**
     * @param relation the relation to set
     */
    public void setRelationType(RelationType relation) {
        this.relationType = relation;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RelatedIdentifier ) {
            return ((RelatedIdentifier)o).getIdentifierType().equals(getIdentifierType()) &&
                   ((RelatedIdentifier)o).getIdentifierValue().equalsIgnoreCase(getIdentifierValue()) &&
                   ((RelatedIdentifier)o).getRelationType().equals(getRelationType());
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.identifierType);
        hash = 83 * hash + Objects.hashCode(this.identifierValue);
        hash = 83 * hash + Objects.hashCode(this.relationType);
        return hash;
    }

}
