/*
 */
package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Objects;
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
     * Enumeration of valid Backfill Types for an Identifier.
     */
    public enum BackfillType{
        Deletion,
        Addition
    }

    /**
     * Enumeration of valid Source Types for an Identifier.
     */
    public enum Source {
        User("User"),
        AutoBackfill("Auto Backfill");

        private final String label;

        private Source(String label) {
            this.label =label;
        }

        public String label() {
            return this.label;
        }
    }

    /**
     * Enumeration of valid Types for an Identifier.
     */
    public enum Type implements Serializable {
        DOI("DOI"),
        URL("URL"),
        AWARD("AWARD");
        
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
        Requires("Requires"),
        Obsoletes("Obsoletes"),
        IsObsoletedBy("Is Obsoleted By");

        private final String label;
        private RelationType inverse;

        static {
            IsCitedBy.inverse = Cites;
            Cites.inverse = IsCitedBy;
            IsSupplementTo.inverse = IsSupplementedBy;
            IsSupplementedBy.inverse = IsSupplementTo;
            IsContinuedBy.inverse = Continues;
            Continues.inverse = IsContinuedBy;
            HasMetadata.inverse = IsMetadataFor;
            IsMetadataFor.inverse = HasMetadata;
            IsNewVersionOf.inverse = IsPreviousVersionOf;
            IsPreviousVersionOf.inverse = IsNewVersionOf;
            IsPartOf.inverse = HasPart;
            HasPart.inverse = IsPartOf;
            IsReferencedBy.inverse = References;
            References.inverse = IsReferencedBy;
            IsDocumentedBy.inverse = Documents;
            Documents.inverse = IsDocumentedBy;
            IsCompiledBy.inverse = Compiles;
            Compiles.inverse = IsCompiledBy;
            IsVariantFormOf.inverse = IsOriginalFormOf;
            IsOriginalFormOf.inverse = IsVariantFormOf;
            IsIdenticalTo.inverse = IsIdenticalTo;
            IsReviewedBy.inverse = Reviews;
            Reviews.inverse = IsReviewedBy;
            IsDerivedFrom.inverse = IsSourceOf;
            IsSourceOf.inverse = IsDerivedFrom;
            IsDescribedBy.inverse = Describes;
            Describes.inverse = IsDescribedBy;
            HasVersion.inverse = IsVersionOf;
            IsVersionOf.inverse = HasVersion;
            IsRequiredBy.inverse = Requires;
            Requires.inverse = IsRequiredBy;
            IsObsoletedBy.inverse = Obsoletes;
            Obsoletes.inverse = IsObsoletedBy;
        }

        private RelationType(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }

        public RelationType inverse() {
            return this.inverse;
        }
    }

    // the specific Type of the Identifier
    private Type identifierType;
    // the value of the Identifier
    private String identifierValue;
    // the Relationship Type (if any) for this Identifier
    private RelationType relationType;
    // source of entry for this Identifier
    private Source source = Source.User;

    public RelatedIdentifier() {

    }

    public RelatedIdentifier(Type idType, String value, RelationType relType) {
        this.identifierType = idType;
        this.identifierValue = value;
        this.relationType = relType;
    }

    public RelatedIdentifier(RelatedIdentifier ri) {
        this(ri.getIdentifierType(),ri.getIdentifierValue(),ri.getRelationType());
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
     * Get the Source for this Identifier
     * @return the Source
     */
    //@Convert(converter = RelatedIdentifierSourceConverter.class)
    @Enumerated (EnumType.STRING)
    @Column (length = 25, name="SOURCE", nullable = false)
    public Source getSource() {
        return this.source;
    }

    /**
     * Add a Source for this Identifier
     * @param s the Source to use
     */
    public void setSource(Source s) {
        this.source = s;
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
