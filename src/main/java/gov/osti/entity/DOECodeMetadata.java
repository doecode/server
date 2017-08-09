package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Reader;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name="metadata")
@JsonIgnoreProperties (ignoreUnknown = true)
public class DOECodeMetadata implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = -909574677603914304L;
	private static final Logger log = LoggerFactory.getLogger(DOECodeMetadata.class.getName());

    // Status values
    public enum Status {
        Saved,
        Published
    }
    
    // Accessibility values
    public enum Accessibility {
        OS,
        ON,
        CS
    }
    
    // Attributes
    private Long codeId;
    private String siteOwnershipCode = null;
    private Boolean openSource = null;
    private String  repositoryLink = null;
    private String landingPage = null;
    private Accessibility accessibility = null;
    
    private String doiStatus = null;

    // set of Access Limitations (Strings)
    private List<String> accessLimitations;
    
    // Child tables -- persons
    private List<Developer> developers;
    private List<Contributor> contributors;

    //  Child tables -- organizations
    private List<SponsoringOrganization> sponsoringOrganizations;
    private List<ContributingOrganization> contributingOrganizations;
    private List<ResearchOrganization> researchOrganizations;

    // Child table -- identifiers
    private List<RelatedIdentifier> relatedIdentifiers;

    private Date releaseDate = null;
    private String softwareTitle = null;
    private String acronym = null;
    private String doi = null;
    private String description = null;
    private String countryOfOrigin = null;
    private String keywords = null;
    private String disclaimers = null;
    private List<String> licenses;
    private String proprietaryUrl = null;
    private String recipientName = null;
    private String recipientEmail = null;
    private String recipientPhone = null;
    private String recipientOrg = null;
    private String siteAccessionNumber = null;
    private String otherSpecialRequirements = null;
    private String owner = null;
    private Status workflowStatus = null;
    
    private String fileName = null;

    // Jackson object mapper
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(Include.NON_NULL);

    //for Gson
    public DOECodeMetadata() {

    }

    /**
     * getJson - Serializes the Metadata Object into a JSON.
     * @return A JsonElement representing the metadata's internal state in JSON
     */
    public JsonNode toJson() {
        return mapper.valueToTree(this);
    }

    /**
     * Parses JSON in the request body of the reader into a DOECodemetadata object.
     * @param reader - A request reader containing JSON in the request body.
     * @return A DOECodeMetadata object representing the data of the JSON in the request body.
     * @throws IOException on JSON parsing errors (IO errors)
     */
    public static DOECodeMetadata parseJson(Reader reader) throws IOException {
        return mapper.readValue(reader, DOECodeMetadata.class);
    }

    /**
     * Primary key for Metadata, the unique identifier value.
     * 
     * @return the unique ID value for this record
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column (name = "CODE_ID")
    public Long getCodeId() {
            return codeId;
    }

    public void setCodeId(Long codeId) {
            this.codeId = codeId;
    }

    @Column (name="SITE_OWNERSHIP_CODE")
    public String getSiteOwnershipCode() {
            return siteOwnershipCode;
    }
    public void setSiteOwnershipCode(String siteOwnershipCode) {
            this.siteOwnershipCode = siteOwnershipCode;
    }

    @Column (name="OPEN_SOURCE")
    public Boolean getOpenSource() {
            return (null==openSource) ? false : openSource;
    }
    public void setOpenSource(Boolean openSource) {
            this.openSource = openSource;
    }
    @Column (name="REPOSITORY_LINK")
    public String getRepositoryLink() {
            return repositoryLink;
    }
    public void setRepositoryLink(String repositoryLink) {
            this.repositoryLink = repositoryLink;
    }

    /**
     * Obtain the set of Access Limitation values for this record.
     * @return a List of Access Limitation values
     */
    @ElementCollection
    @CollectionTable(
            name = "ACCESS_LIMITATIONS",
            joinColumns=@JoinColumn(name="CODE_ID")
    )
    @Column (name="ACCESS_LIMITATION")
    public List<String> getAccessLimitations() {
        return this.accessLimitations;
    }
    
    /**
     * Set the Access Limitations for this record.
     * @param limitations a set of Access Limitations to store
     */
    public void setAccessLimitations(List<String> limitations) {
        this.accessLimitations = limitations;
    }
    
    /**
     * Get all the Contributors for this Metadata.
     * @return the Contributor List
     */
    @OneToMany (cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn (name="OWNER_ID", referencedColumnName = "CODE_ID")
    public List<Contributor> getContributors() {
        return this.contributors;
    }

    /**
     * Get all the Sponsoring Organizations for this Metadata
     * @return a List of Sponsoring Organizations
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn (name ="OWNER_ID", referencedColumnName = "CODE_ID")
    public List<SponsoringOrganization> getSponsoringOrganizations() {
        return this.sponsoringOrganizations;
    }

    /**
     * Get all the Contributing Organizations
     * @return the List of Contributing Organizations
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn (name = "OWNER_ID", referencedColumnName = "CODE_ID")
    public List<ContributingOrganization> getContributingOrganizations() {
        return this.contributingOrganizations;
    }
    
    /**
     * Get all the Research Organizations
     * @return a List of Research Organizations
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn (name="OWNER_ID", referencedColumnName = "CODE_ID")
    public List<ResearchOrganization> getResearchOrganizations() {
        return this.researchOrganizations;
    }

    @Column (name = "SOFTWARE_TITLE", length = 1000)
    public String getSoftwareTitle() {
            return softwareTitle;
    }
    public void setSoftwareTitle(String softwareTitle) {
            this.softwareTitle = softwareTitle;
    }
    public String getAcronym() {
            return acronym;
    }
    public void setAcronym(String acronym) {
            this.acronym = acronym;
    }
    public String getDoi() {
            return doi;
    }
    public void setDoi(String doi) {
            this.doi = doi;
    }
    @Column (length = 4000, name = "description")
    public String getDescription() {
            return description;
    }
    public void setDescription(String description) {
            this.description = description;
    }

    public void setRelatedIdentifiers(List<RelatedIdentifier> identifiers) {
        this.relatedIdentifiers = identifiers;
    }

    @ElementCollection
    @CollectionTable(
            name="RELATED_IDENTIFIERS",
            joinColumns=@JoinColumn(name="CODE_ID")
    )
    public List<RelatedIdentifier> getRelatedIdentifiers() {
        return this.relatedIdentifiers;
    }

    @Column (name = "COUNTRY_OF_ORIGIN")
    public String getCountryOfOrigin() {
            return countryOfOrigin;
    }
    public void setCountryOfOrigin(String countryOfOrigin) {
            this.countryOfOrigin = countryOfOrigin;
    }
    @Column (length = 500)
    public String getKeywords() {
            return keywords;
    }
    public void setKeywords(String keywords) {
            this.keywords = keywords;
    }
    @Column (length = 3000)
    public String getDisclaimers() {
            return disclaimers;
    }
    public void setDisclaimers(String disclaimers) {
            this.disclaimers = disclaimers;
    }
    @ElementCollection
    @CollectionTable(
            name = "LICENSES",
            joinColumns=@JoinColumn(name="CODE_ID")
    )
    @Column (name = "LICENSE")
    public List<String> getLicenses() {
            return licenses;
    }
    public void setLicenses(List<String> licenses) {
            this.licenses = licenses;
    }
     

    @Column (name="PROPRIETARY_URL")
    public String getProprietaryUrl() {
            return proprietaryUrl;
    }

    public void setProprietaryUrl(String proprietaryUrl) {
            this.proprietaryUrl = proprietaryUrl;
    }

	/**
     * Get all the Developers for this Metadata
     * @return the List of Developers
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn (name="OWNER_ID", referencedColumnName = "CODE_ID")
    public List<Developer> getDevelopers() {
            return developers;
    }

    /**
     * Set the ContributingOrganization List
     * @param list the List to set
     */
    public void setContributingOrganizations(List<ContributingOrganization> list) {
        this.contributingOrganizations = list;
    }

    /**
     * Set the ResearchOrganization List
     * @param list the List to set
     */
    public void setResearchOrganizations(List<ResearchOrganization> list) {
        this.researchOrganizations = list;
    }

    /**
     * Set the SponsoringOrganization List.
     * @param list the List to set
     */
    public void setSponsoringOrganizations(List<SponsoringOrganization> list) {
        this.sponsoringOrganizations = list;
    }

    /**
     * Associate the List of Contributor Objects to this metadata.
     * 
     * @param list the List of Contributor items.
     */
    public void setContributors(List<Contributor> list) {
        this.contributors = list;
    }

    /**
     * Add entire List at once; make sure to keep Place up to date properly.
     * 
     * @param devlist a List of Developers to set
     */
    public void setDevelopers(List<Developer> devlist) {
        this.developers = devlist;
    }

    @Column (name = "RECIPIENT_NAME")
    public String getRecipientName() {
            return recipientName;
    }
    public void setRecipientName(String recipientName) {
            this.recipientName = recipientName;
    }
    @Column (name="RECIPIENT_EMAIL")
    public String getRecipientEmail() {
            return recipientEmail;
    }
    public void setRecipientEmail(String recipientEmail) {
            this.recipientEmail = recipientEmail;
    }
    @Column (name="RECIPIENT_PHONE")
    public String getRecipientPhone() {
            return recipientPhone;
    }
    public void setRecipientPhone(String recipientPhone) {
            this.recipientPhone = recipientPhone;
    }
    @Column (name = "RECIPIENT_ORGANIZATION")
    public String getRecipientOrg() {
            return recipientOrg;
    }
    public void setRecipientOrg(String recipientOrg) {
            this.recipientOrg = recipientOrg;
    }

    @Column (name="SITE_ACCESSION_NUMBER")
    public String getSiteAccessionNumber() {
            return siteAccessionNumber;
    }
    public void setSiteAccessionNumber(String siteAccessionNumber) {
            this.siteAccessionNumber = siteAccessionNumber;
    }

    @Column (name="OTHER_SPECIAL_REQUIREMENTS", length = 1500)
    public String getOtherSpecialRequirements() {
            return otherSpecialRequirements;
    }
    public void setOtherSpecialRequirements(String otherSpecialRequirements) {
            this.otherSpecialRequirements = otherSpecialRequirements;
    }

    /**
     * Obtain the WORKFLOW STATUS on this record (initially New, then Saved; after
     * a record is Published or Submitted, it may no longer be Saved.)
     * 
     * @return the Status value for this record
     */
    @Enumerated (EnumType.STRING)
    @Column (name="WORKFLOW_STATUS")
    public Status getWorkflowStatus() {
        return workflowStatus;
    }
    
    /**
     * Set the WORKFLOW STATUS on this record.
     * @param status the Status value to set
     */
    public void setWorkflowStatus(Status status) {
        workflowStatus = status;
    }
    
    public void setReleaseDate(Date date) {
        this.releaseDate = date;
    }
    
    @Column (name="release_date")
    @Temporal(javax.persistence.TemporalType.DATE)
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "EST")
    public Date getReleaseDate() {
        return this.releaseDate;
    }

    @Column (name="LANDING_PAGE")
	public String getLandingPage() {
		return landingPage;
	}

	public void setLandingPage(String landingPage) {
		this.landingPage = landingPage;
	}

    @Column (name="DOI_STATUS")
	public String getDoiStatus() {
		return doiStatus;
	}

	public void setDoiStatus(String doiStatus) {
		this.doiStatus = doiStatus;
	}

        @Enumerated (EnumType.STRING)
        @Column (name = "ACCESSIBLIITY")
	public Accessibility getAccessibility() {
		return accessibility;
	}

	public void setAccessibility(Accessibility accessibility) {
		this.accessibility = accessibility;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}
	
        /**
         * Set the FILE NAME if any for archive.
         * 
         * @param name the ABSOLUTE PATH name to the archive file, if any
         */
        public void setFileName(String name) {
            this.fileName = name;
        }
        
        /**
         * Get the FILE NAME for this project, if any.
         * 
         * For serialization purposes, DO NOT show the full path name, only the
         * base file name.
         * 
         * @return the FILE NAME
         */
        @Column (length = 500, name = "FILE_NAME")
        @JsonSerialize (using = FileNameSerializer.class)
        public String getFileName() {
            return this.fileName;
        }
}
