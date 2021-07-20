package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.File;
import java.io.Reader;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import gov.osti.entity.BiblioLink;
import gov.osti.listeners.DoeServletContextListener;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.TimeZone;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name="metadata")
@JsonIgnoreProperties (ignoreUnknown = true)
@NamedQueries ({
    @NamedQuery (name = "DOECodeMetadata.findByDoi", query = "SELECT m FROM DOECodeMetadata m WHERE m.doi = :doi"),
    @NamedQuery (name = "DOECodeMetadata.findByStatus", query = "SELECT m FROM DOECodeMetadata m WHERE m.workflowStatus = :status"),
    @NamedQuery (name = "DOECodeMetadata.findByDoiAndRi", query = "SELECT m FROM DOECodeMetadata m JOIN m.relatedIdentifiers r WHERE m.doi = :doi AND r.identifierType = :type AND r.identifierValue = :value AND r.relationType = :relType")
})
@XmlRootElement (name = "metadata")
@JsonRootName (value = "metadata")
public class DOECodeMetadata implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -909574677603914304L;
    private static final Logger log = LoggerFactory.getLogger(DOECodeMetadata.class.getName());

    // get the SITE URL base for applications
    private static String SITE_URL = DoeServletContextListener.getConfigurationProperty("site.url");

    /**
     * Record states/work flow:
     * Saved - stored to the database without validation
     * Submitted - validated to business logic rules, and/or sent to OSTI
     * Approved - ready to be sent to SOLR/search services
     * Announced - sent to OSTI
     */
    public enum Status {
        Saved,
        Submitted,
        Announced,
        Approved
    }

    // Accessibility values
    public enum Accessibility {
        OS("Open Source"),
        ON("Open Source, No Public Access"),
        CS("Closed Source"),
        CO("Closed Source, OSTI Hosted");

        private final String label;

        private Accessibility(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }

    /**
     * Enumerate known or accepted license values.
     */
    public enum License {
        Other("Other", "Other"),
        Apache("Apache License 2.0", "Apache License 2.0"),
        GNU3("GNU General Public License v3.0","GNU General Public License v3.0"),
        MIT("MIT License", "MIT License"),
        BSD2("BSD 2-clause \"Simplified\" License", "BSD 2-clause \"Simplified\" License"),
        BSD3("BSD 3-clause \"New\" or \"Revised\" License","BSD 3-clause \"New\" or \"Revised\" License"),
        Eclipse1("Eclipse Public License 1.0","Eclipse Public License 1.0"),
        GNUAffero3("GNU Affero General Public License v3.0","GNU Affero General Public License v3.0"),
        GNUpublic2("GNU General Public License v2.0","GNU General Public License v2.0"),
        GNUpublic21("GNU General Public License v2.1","GNU General Public License v2.1"),
        GNUlesser21("GNU Lesser General Public License v2.1","GNU Lesser General Public License v2.1"),
        GNUlesser3("GNU Lesser General Public License v3.0","GNU Lesser General Public License v3.0"),
        MOZ2("Mozilla Public License 2.0","Mozilla Public License 2.0"),
        Unlicense("The Unlicense", "The Unlicense");
        
        private final String label;
        private final String value;

        private static final Set<String> values = new HashSet<String>(License.values().length);

        static{
            for(License f: License.values())
                values.add(f.value());
        }
        
        private License(String label, String value) {
            this.label =label;
            this.value =value;
        }
        
        public String value() {
            return this.value;
        }
        
        public String label() {
            return this.label;
        }

        public static boolean contains(String value){
            return values.contains(value);
        }
    }
    
    /**
     * Define the valid SOFTWARE TYPES.
     */
    public enum Type {
        S ("Scientific"),
        B ("Business");
        
        private final String label;
        
        private Type(String label) {
            this.label = label;
        }
        
        public String label() {
            return this.label;
        }
    }
    
    // Attributes
    private Long codeId;
    private String siteOwnershipCode = null;
    private Boolean openSource = null;
    private String  repositoryLink = null;
    private String landingPage = null;
    private Accessibility accessibility = null;
    // the SOFTWARE TYPE
    private Type softwareType;
    // set of Access Limitations (Strings)
    @JacksonXmlElementWrapper (localName = "accessLimitations")
    @JacksonXmlProperty (localName = "accessLimitation")
    private List<String> accessLimitations;
    
    // Child tables -- persons
    @JacksonXmlElementWrapper (localName = "developers")
    @JacksonXmlProperty (localName = "developer")
    private List<Developer> developers;
    @JacksonXmlElementWrapper (localName = "contributors")
    @JacksonXmlProperty (localName = "contributor")
    private List<Contributor> contributors;

    //  Child tables -- organizations
    @JacksonXmlElementWrapper (localName = "sponsoringOrganizations")
    @JacksonXmlProperty (localName = "sponsoringOrganization")
    private List<SponsoringOrganization> sponsoringOrganizations;
    @JacksonXmlElementWrapper (localName = "contributingOrganizations")
    @JacksonXmlProperty (localName = "contributingOrganization")
    private List<ContributingOrganization> contributingOrganizations;
    @JacksonXmlElementWrapper (localName = "researchOrganizations")
    @JacksonXmlProperty (localName = "researchOrganization")
    private List<ResearchOrganization> researchOrganizations;

    // Child table -- identifiers
    @JacksonXmlElementWrapper (localName = "relatedIdentifiers")
    @JacksonXmlProperty (localName = "relatedIdentifier")
    private List<RelatedIdentifier> relatedIdentifiers;

    // Child table -- identifiers
    @JacksonXmlElementWrapper (localName = "awardDois")
    @JacksonXmlProperty (localName = "awardDoi")
    private List<Award> awardDois;

    private Date releaseDate;
    private String softwareTitle = null;
    private String acronym = null;
    private String doi = null;
    private String description = null;

    @JacksonXmlElementWrapper (localName = "programmingLanguages")
    @JacksonXmlProperty (localName = "programmingLanguage")
    private List<String> programmingLanguages;

    private String versionNumber = null;
    private String documentationUrl = null;
    private String countryOfOrigin = null;
    private String keywords = null;

    @JacksonXmlElementWrapper (localName = "projectKeywords")
    @JacksonXmlProperty (localName = "projectKeyword")
    private List<String> projectKeywords;

    private String disclaimers = null;

    @JacksonXmlElementWrapper (localName = "licenses")
    @JacksonXmlProperty (localName = "license")
    private List<String> licenses;

    private String proprietaryUrl = null;
    private String licenseContactEmail = null;
    private String recipientName = null;
    private String recipientEmail = null;
    private String recipientPhone = null;
    private String recipientOrg = null;
    private String siteAccessionNumber = null;
    private String otherSpecialRequirements = null;
    private String owner = null;
    private Status workflowStatus = null;

    private String fileName = null;
    private String containerName = null;

    // administrative dates
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date dateRecordAdded;
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date dateRecordUpdated;

    // determine whether or not the RELEASE DATE was changed
    private transient boolean hasSetReleaseDate=false;

    @JacksonXmlElementWrapper (localName = "links")
    @JacksonXmlProperty (localName = "link")
    private transient List<BiblioLink> links; 

    private boolean isMigration = false;
    private String lastEditor = null;

    // Jackson object mapper
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(Include.NON_NULL)
            .setTimeZone(TimeZone.getDefault());

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
    @Size (max = 255, message = "Repository link is limited to 255 characters.")
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
        this.accessLimitations = this.CleanList(limitations);
    }
    
    /**
     * Get all the Contributors for this Metadata.
     * @return the Contributor List
     */
    @OneToMany (cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn (name="OWNER_ID", referencedColumnName = "CODE_ID")
    @Valid
    @OrderColumn(name="ordinal_position")
    public List<Contributor> getContributors() {
        return this.contributors;
    }

    /**
     * Get all the Sponsoring Organizations for this Metadata
     * @return a List of Sponsoring Organizations
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn (name ="OWNER_ID", referencedColumnName = "CODE_ID")
    @Valid
    public List<SponsoringOrganization> getSponsoringOrganizations() {
        return this.sponsoringOrganizations;
    }

    /**
     * Get all the Contributing Organizations
     * @return the List of Contributing Organizations
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn (name = "OWNER_ID", referencedColumnName = "CODE_ID")
    @Valid
    public List<ContributingOrganization> getContributingOrganizations() {
        return this.contributingOrganizations;
    }
    
    /**
     * Get all the Research Organizations
     * @return a List of Research Organizations
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn (name="OWNER_ID", referencedColumnName = "CODE_ID")
    @Valid
    public List<ResearchOrganization> getResearchOrganizations() {
        return this.researchOrganizations;
    }

    @Size (max = 1000, message = "Software title is limited to 1000 characters.")
    @Column (name = "SOFTWARE_TITLE", length = 1000)
    public String getSoftwareTitle() {
            return softwareTitle;
    }
    public void setSoftwareTitle(String softwareTitle) {
            this.softwareTitle = softwareTitle;
    }
    @Size (max = 255, message = "Acronym is limited to 255 characters.")
    public String getAcronym() {
            return acronym;
    }
    public void setAcronym(String acronym) {
            this.acronym = acronym;
    }
    @Size (max = 255, message = "DOI is limited to 255 characters.")
    public String getDoi() {
            return doi;
    }
    public void setDoi(String doi) {
            this.doi = doi;
    }
    @Size (max = 4000, message = "Description is limited to 4000 characters.")
    @Column (length = 4000, name = "description")
    public String getDescription() {
            return description;
    }
    public void setDescription(String description) {
            this.description = description;
    }

    @ElementCollection
    @CollectionTable(
            name = "PROGRAMMING_LANGUAGES",
            joinColumns=@JoinColumn(name="CODE_ID")
    )
    @Column (name = "PROGRAMMING_LANGUAGE")
    public List<String> getProgrammingLanguages() {
            return programmingLanguages;
    }
    public void setProgrammingLanguages(List<String> programmingLanguages) {
            this.programmingLanguages = this.CleanList(programmingLanguages);
    }

    @Size (max = 50, message = "Version Number is limited to 50 characters.")
    @Column (name="VERSION_NUMBER", length = 50)
    public String getVersionNumber() {
            return versionNumber;
    }

    public void setVersionNumber(String versionNumber) {
            this.versionNumber = versionNumber;
    }

    @Size (max = 255, message = "Documentation URL is limited to 255 characters.")
    @Column (name="DOCUMENTATION_URL")
    public String getDocumentationUrl() {
            return documentationUrl;
    }

    public void setDocumentationUrl(String documentationUrl) {
            this.documentationUrl = documentationUrl;
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

    public void setAwardDois(List<Award> awardDois) {
        this.awardDois = awardDois;
    }

    @ElementCollection
    @CollectionTable(
            name="AWARD_DOIS",
            joinColumns=@JoinColumn(name="CODE_ID")
    )
    public List<Award> getAwardDois() {
        return this.awardDois;
    }

    @Size (max = 255, message = "Country is limited to 255 characters.")
    @Column (name = "COUNTRY_OF_ORIGIN")
    public String getCountryOfOrigin() {
            return countryOfOrigin;
    }
    public void setCountryOfOrigin(String countryOfOrigin) {
            this.countryOfOrigin = countryOfOrigin;
    }
    @Size (max = 500, message = "Keywords are limited to 500 characters.")
    @Column (length = 500)
    public String getKeywords() {
            return keywords;
    }
    public void setKeywords(String keywords) {
            this.keywords = keywords;
    }

    @ElementCollection
    @CollectionTable(
            name = "PROJECT_KEYWORDS",
            joinColumns=@JoinColumn(name="CODE_ID")
    )
    @Column (name = "PROJECT_KEYWORD")
    public List<String> getProjectKeywords() {
            return projectKeywords;
    }
    public void setProjectKeywords(List<String> projectKeywords) {
            this.projectKeywords = this.CleanList(projectKeywords);
    }

    @Size (max = 3000, message = "Disclaimers are limited to 3000 characters.")
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
            this.licenses = this.CleanList(licenses);
    }
     

    @Size (max = 255, message = "Proprietary URL is limited to 255 characters.")
    @Column (name="PROPRIETARY_URL")
    public String getProprietaryUrl() {
            return proprietaryUrl;
    }

    public void setProprietaryUrl(String proprietaryUrl) {
            this.proprietaryUrl = proprietaryUrl;
    }
     

    @Size (max = 255, message = "License Contact email is limited to 255 characters.")
    @Column (name="license_contact_email")
    public String getLicenseContactEmail() {
            return licenseContactEmail;
    }
    public void setLicenseContactEmail(String licenseContactEmail) {
            this.licenseContactEmail = licenseContactEmail;
    }

	/**
     * Get all the Developers for this Metadata
     * @return the List of Developers
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn (name="OWNER_ID", referencedColumnName = "CODE_ID")
    @Valid
    @OrderColumn(name="ordinal_position")
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

    @Size (max = 255, message = "Recipient name is limited to 255 characters.")
    @Column (name = "RECIPIENT_NAME", length = 255)
    public String getRecipientName() {
            return recipientName;
    }
    public void setRecipientName(String recipientName) {
            this.recipientName = recipientName;
    }
    @Size (max = 255, message = "Recipient email is limited to 255 characters.")
    @Column (name="RECIPIENT_EMAIL")
    public String getRecipientEmail() {
            return recipientEmail;
    }
    public void setRecipientEmail(String recipientEmail) {
            this.recipientEmail = recipientEmail;
    }
    @Size (max = 255, message = "Recipient phone is limited to 255 characters.")
    @Column (name="RECIPIENT_PHONE")
    public String getRecipientPhone() {
            return recipientPhone;
    }
    public void setRecipientPhone(String recipientPhone) {
            this.recipientPhone = recipientPhone;
    }
    @Size (max = 255, message = "Recipient organization is limited to 255 characters.")
    @Column (name = "RECIPIENT_ORGANIZATION")
    public String getRecipientOrg() {
            return recipientOrg;
    }
    public void setRecipientOrg(String recipientOrg) {
            this.recipientOrg = recipientOrg;
    }

    @Size (max = 25, message = "Site Accession number is limited to 25 characters.")
    @Column (name="SITE_ACCESSION_NUMBER")
    public String getSiteAccessionNumber() {
            return siteAccessionNumber;
    }
    public void setSiteAccessionNumber(String siteAccessionNumber) {
            this.siteAccessionNumber = siteAccessionNumber;
    }

    @Size (max = 1500, message = "Other special requirements field is limited to 255 characters.")
    @Column (name="OTHER_SPECIAL_REQUIREMENTS", length = 1500)
    public String getOtherSpecialRequirements() {
            return otherSpecialRequirements;
    }
    public void setOtherSpecialRequirements(String otherSpecialRequirements) {
            this.otherSpecialRequirements = otherSpecialRequirements;
    }

    /**
     * Obtain the WORKFLOW STATUS on this record (initially New, then Saved; after
     * a record is Announced or Submitted, it may no longer be Saved.)
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
    
    /**
     * Set the RELEASE DATE value.
     * 
     * @param date the date to set
     */
    public void setReleaseDate(Date date) {
        this.releaseDate = date;
        // set the fact we have called this method to set the date value
        this.hasSetReleaseDate=true;
    }
    
    @Column (name="release_date")
    @Temporal(javax.persistence.TemporalType.DATE)
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public Date getReleaseDate() {
        return this.releaseDate;
    }

    @Size (max = 255, message = "Landing page URL is limited to 255 characters.")
    @Column (name="LANDING_PAGE")
	public String getLandingPage() {
		return landingPage;
	}

	public void setLandingPage(String landingPage) {
		this.landingPage = landingPage;
	}

    @Enumerated (EnumType.STRING)
    @Column (name = "ACCESSIBLIITY")
	public Accessibility getAccessibility() {
		return accessibility;
	}

	public void setAccessibility(Accessibility accessibility) {
		this.accessibility = accessibility;
	}

    /**
     * Get the OWNER of a record.  May not change once set.
     * @return the OWNER (email) of the original record
     */
    @Column (name="OWNER", insertable = true, updatable = false)
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
        owner = owner != null ? owner.toLowerCase() : owner;
		this.owner = owner;
	}

    /**
     * Set the FILE NAME if any for archive.
     *
     * @param name the ABSOLUTE PATH name to the archive file, if any
     */
    public void setFileName(String name) {
        this.fileName = name == null ? null : name.substring(name.lastIndexOf(File.separator) + 1);
    }

    /**
     * Get the FILE NAME for this project, if any.
     *
     * For serialization purposes, DO NOT show the full path name, only the
     * base file name.
     *
     * @return the FILE NAME
     */
    @Column(length = 500, name = "FILE_NAME")
    public String getFileName() {
        return this.fileName == null ? null : this.fileName.substring(this.fileName.lastIndexOf(File.separator) + 1);
    }

    /**
     * Set the CONTAINER NAME if any for archive.
     *
     * @param name the ABSOLUTE PATH name to the archive file, if any
     */
    public void setContainerName(String name) {
        this.containerName = name == null ? null : name.substring(name.lastIndexOf(File.separator) + 1);
    }

    /**
     * Get the CONTAINER NAME for this project, if any.
     *
     * For serialization purposes, DO NOT show the full path name, only the
     * base container name.
     *
     * @return the CONTAINER NAME
     */
    @Column(length = 500, name = "CONTAINER_NAME")
    public String getContainerName() {
        return this.containerName == null ? null : this.containerName.substring(this.containerName.lastIndexOf(File.separator) + 1);
    }

    /**
     * @return the dateRecordAdded
     */
    @Basic(optional = false)
    @Column(name = "date_record_added", insertable = true, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getDateRecordAdded() {
        return dateRecordAdded;
    }

    /**
     * @param dateRecordAdded the dateRecordAdded to set
     */
    public void setDateRecordAdded(Date dateRecordAdded) {
        this.dateRecordAdded = dateRecordAdded;
    }

    public void setDateRecordAdded() {
        setDateRecordAdded(new Date());
    }

    /**
     * @return the dateRecordUpdated
     */
    @Basic(optional = false)
    @Column(name = "date_record_updated", insertable = true, updatable = true)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getDateRecordUpdated() {
        return dateRecordUpdated;
    }

    /**
     * @param dateRecordUpdated the dateRecordUpdated to set
     */
    public void setDateRecordUpdated(Date dateRecordUpdated) {
        this.dateRecordUpdated = dateRecordUpdated;
    }

    public void setDateRecordUpdated() {
        setDateRecordUpdated(new Date());
    }

    @Column (name="IS_MIGRATION")
	public boolean getIsMigration() {
		return isMigration;
	}

	public void setIsMigration(boolean isMigration) {
		this.isMigration = isMigration;
	}

    /**
     * Get the LAST EDITOR user of a record.
     * @return the OWNER (email) of the record editor
     */
    @Column (name="LAST_EDITOR")
	public String getLastEditor() {
		return lastEditor;
	}

	public void setLastEditor(String owner) {
        owner = owner != null ? owner.toLowerCase() : owner;
		this.lastEditor = owner;
	}

    /**
     * Method called when a record is first created. Sets dates added and
     * updated.
     */
    @PrePersist
    void createdAt() {
        setDateRecordAdded();
        setDateRecordUpdated();
    }

    /**
     * Method called when the record is updated.
     */
    @PreUpdate
    void updatedAt() {
        setDateRecordUpdated();
    }

    /**
     * Determine whether or not the RELEASE DATE was changed (possibly to null).
     * Prevents Bean utilities from "losing" the changed release date if it gets
     * "unset".
     *
     * @return true if setReleaseDate() has been called, false if not
     */
    @JsonIgnore
    public boolean hasSetReleaseDate() {
        return hasSetReleaseDate;
    }

    /**
     * Get biblio links.
     *
     * @return list of links for this record
     */
    public List<BiblioLink> getLinks() {
        String biblioLink = SITE_URL + "/biblio/" + this.codeId;

        this.links = new ArrayList<>();
        BiblioLink citationLink = new BiblioLink("citation", biblioLink);
        this.links.add(citationLink);

        return this.links;
    }

    /**
     * Obtain the SOFTWARE TYPE: one of Type.S (scientific) or Type.B (Business)
     *
     * @return the type the SOFTWARE TYPE
     */
    @Basic(optional = false)
    @Valid
    @Column(name = "SOFTWARE_TYPE")
    @Enumerated(EnumType.STRING)
    public Type getSoftwareType() {
        return softwareType;
    }

    /**
     * Set the SOFTWARE TYPE value.
     *
     * @param type the type to set
     */
    public void setSoftwareType(Type type) {
        this.softwareType = type;
    }

    protected List<String> CleanList(List<String> list) {
        if (list != null) {
            // remove empty data
            int cnt = list.size();
            for (int i = cnt - 1; i >= 0; i--) {
                    String s = list.get(i);
                    if (StringUtils.isBlank(s))
                    list.remove(i);
            }
        }

        return list;
    }
}
