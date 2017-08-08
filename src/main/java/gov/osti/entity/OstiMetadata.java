/*
 */
package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.neovisionaries.i18n.CountryCode;
import java.io.IOException;
import java.io.Reader;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Implement an Entity class for exchanging information with OSTI's ELINK service.
 * 
 * @author ensornl
 */
@JsonIgnoreProperties (ignoreUnknown = true)
public class OstiMetadata {
    private Long codeId;
    private Long ostiId;
    private Long softId;
    private String siteOwnershipCode;
    private String keywords;
    private String countryPublicationCode;
    private String softwareTitle;
    private String acronym;
    private String contributingOrganizations;
    private String doeContractNumbers;
    private Date issuanceDate;
    private String nondoeContractNumbers;
    private String otherIdentifyingNumbers;
    private String submittingOrganization;
    private String sponsoringOrganization;
    private String description;
    private String programmingLanguage;
    private String mediaUrl;
    private String openSourceFlag;
    private String contactName;
    private String contactEmail;
    private String contactOrg;
    private String contactPhone;
    private List<Developer> softwareAuthorDetails;
    private String doi;
    private String doiInfix;
    
    // Jackson object mapper
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    
    public OstiMetadata() {
        
    }
    
    /**
     * Read a Reader JSON into a new OstiMetadata object.
     * 
     * @param reader the Reader to read JSON from
     * @return an OstiMetadata Object parsed from that JSON
     * @throws IOException on unexpected errors
     */
    public static OstiMetadata fromJson(Reader reader) throws IOException {
        return mapper.readValue(reader, OstiMetadata.class);
    }
    
    /**
     * Attempt to locate a CountryCode based on the passed-in name (in case-insensitive
     * search).
     * 
     * @param name the COUNTRY NAME
     * @return a List (possibly empty) of CountryCode values if any matched
     */
    public static List<CountryCode> findCountryByName(String name) {
        return CountryCode.findByName(Pattern.compile(name, Pattern.CASE_INSENSITIVE));
    }
    
    /**
     * Copy the relevant attributes from DOECodeMetadata into a submission object
     * for OSTI's ELINK service.
     * 
     * @param md the DOECodeMetadata to source data from
     */
    public void set(DOECodeMetadata md) {
        setCodeId(md.getCodeId());
        setSiteOwnershipCode(md.getSiteOwnershipCode());
        setKeywords(md.getKeywords());
        // DOECODE stores COUNTRY NAME; convert to two-character standard COUNTRY CODE
        List<CountryCode> countries = findCountryByName(md.getCountryOfOrigin());
        // "ZZ" is the default unknown value for ELINK
        setCountryPublicationCode ((countries.isEmpty()) ? "ZZ" : countries.get(0).getAlpha2());
        setSoftwareTitle(md.getSoftwareTitle());
        setAcronym(md.getAcronym());
        
        StringBuilder corgs = new StringBuilder();
        if (null!=md.getContributingOrganizations()) {
            for ( ContributingOrganization o : md.getContributingOrganizations() ) {
                if (corgs.length()>0)
                    corgs.append("; ");
                corgs.append(o.getOrganizationName());
            }
        }
        setContributingOrganizations(corgs.toString());
        
        StringBuilder doenumber = new StringBuilder(),
                      nondoenumber = new StringBuilder(),
                      sponsororgs = new StringBuilder();
        for ( SponsoringOrganization o : md.getSponsoringOrganizations() ) {
            if (sponsororgs.length()>0) sponsororgs.append("; ");
            sponsororgs.append(o.getOrganizationName());
            // separate DOE and non-DOE award numbers
            if (o.isDOE()) {
                for ( FundingIdentifier fid : o.getFundingIdentifiers() ) {
                    if ( FundingIdentifier.Type.AwardNumber == fid.getIdentifierType() ) {
                        if (doenumber.length()>0)
                            doenumber.append("; ");
                        doenumber.append(fid.getIdentifierValue());
                    }
                }
            } else {
                for ( FundingIdentifier fid : o.getFundingIdentifiers() ) {
                    if (FundingIdentifier.Type.AwardNumber==fid.getIdentifierType()) {
                        if (nondoenumber.length()>0) nondoenumber.append("; ");
                        nondoenumber.append(fid.getIdentifierValue());
                    }
                }
            }
        }
        setDoeContractNumbers(doenumber.toString());
        setNondoeContractNumbers(nondoenumber.toString());
        setIssuanceDate(md.getReleaseDate());
        
        setSponsoringOrganization(sponsororgs.toString());
        setDescription(md.getDescription());
        setMediaUrl(md.getRepositoryLink());
        setOpenSourceFlag((md.getOpenSource()) ? "Y" : "N");
        setContactEmail(md.getRecipientEmail());
        setContactOrg(md.getRecipientOrg());
        setContactPhone(md.getRecipientPhone());
        setContactName(md.getRecipientName());
        setDoi(md.getDoi());
        setSoftwareAuthorDetails(md.getDevelopers());
    }

    /**
     * @return the siteOwnershipCode
     */
    public String getSiteOwnershipCode() {
        return siteOwnershipCode;
    }

    /**
     * @param siteOwnershipCode the siteOwnershipCode to set
     */
    public void setSiteOwnershipCode(String siteOwnershipCode) {
        this.siteOwnershipCode = siteOwnershipCode;
    }

    /**
     * @return the keywords
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * @param keywords the keywords to set
     */
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    /**
     * @return the countryPublicationCode
     */
    public String getCountryPublicationCode() {
        return countryPublicationCode;
    }

    /**
     * @param countryPublicationCode the countryPublicationCode to set
     */
    public void setCountryPublicationCode(String countryPublicationCode) {
        this.countryPublicationCode = countryPublicationCode;
    }

    /**
     * @return the softwareTitle
     */
    public String getSoftwareTitle() {
        return softwareTitle;
    }

    /**
     * @param softwareTitle the softwareTitle to set
     */
    public void setSoftwareTitle(String softwareTitle) {
        this.softwareTitle = softwareTitle;
    }

    /**
     * @return the acronym
     */
    public String getAcronym() {
        return acronym;
    }

    /**
     * @param acronym the acronym to set
     */
    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    /**
     * @return the contributingOrganizations
     */
    public String getContributingOrganizations() {
        return contributingOrganizations;
    }

    /**
     * @param contributingOrganizations the contributingOrganizations to set
     */
    public void setContributingOrganizations(String contributingOrganizations) {
        this.contributingOrganizations = contributingOrganizations;
    }

    /**
     * @return the doeContractNumbers
     */
    public String getDoeContractNumbers() {
        return doeContractNumbers;
    }

    /**
     * @param doeContractNumbers the doeContractNumbers to set
     */
    public void setDoeContractNumbers(String doeContractNumbers) {
        this.doeContractNumbers = doeContractNumbers;
    }

    /**
     * @return the issuanceDate
     */
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "EST")
    public Date getIssuanceDate() {
        return issuanceDate;
    }

    /**
     * @param issuanceDate the issuanceDate to set
     */
    public void setIssuanceDate(Date issuanceDate) {
        this.issuanceDate = issuanceDate;
    }

    /**
     * @return the nondoeContractNumbers
     */
    public String getNondoeContractNumbers() {
        return nondoeContractNumbers;
    }

    /**
     * @param nondoeContractNumbers the nondoeContractNumbers to set
     */
    public void setNondoeContractNumbers(String nondoeContractNumbers) {
        this.nondoeContractNumbers = nondoeContractNumbers;
    }

    /**
     * @return the otherIdentifyingNumbers
     */
    public String getOtherIdentifyingNumbers() {
        return otherIdentifyingNumbers;
    }

    /**
     * @param otherIdentifyingNumbers the otherIdentifyingNumbers to set
     */
    public void setOtherIdentifyingNumbers(String otherIdentifyingNumbers) {
        this.otherIdentifyingNumbers = otherIdentifyingNumbers;
    }

    /**
     * @return the submittingOrganization
     */
    public String getSubmittingOrganization() {
        return submittingOrganization;
    }

    /**
     * @param submittingOrganization the submittingOrganization to set
     */
    public void setSubmittingOrganization(String submittingOrganization) {
        this.submittingOrganization = submittingOrganization;
    }

    /**
     * @return the sponsoringOrganization
     */
    public String getSponsoringOrganization() {
        return sponsoringOrganization;
    }

    /**
     * @param sponsoringOrganization the sponsoringOrganization to set
     */
    public void setSponsoringOrganization(String sponsoringOrganization) {
        this.sponsoringOrganization = sponsoringOrganization;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the programmingLanguage
     */
    public String getProgrammingLanguage() {
        return programmingLanguage;
    }

    /**
     * @param programmingLanguage the programmingLanguage to set
     */
    public void setProgrammingLanguage(String programmingLanguage) {
        this.programmingLanguage = programmingLanguage;
    }

    /**
     * @return the mediaUrl
     */
    public String getMediaUrl() {
        return mediaUrl;
    }

    /**
     * @param mediaUrl the mediaUrl to set
     */
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    /**
     * @return the openSourceFlag
     */
    public String getOpenSourceFlag() {
        return openSourceFlag;
    }

    /**
     * @param openSourceFlag the openSourceFlag to set
     */
    public void setOpenSourceFlag(String openSourceFlag) {
        this.openSourceFlag = openSourceFlag;
    }

    /**
     * @return the contactEmail
     */
    public String getContactEmail() {
        return contactEmail;
    }

    /**
     * @param contactEmail the contactEmail to set
     */
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    /**
     * @return the contactOrg
     */
    public String getContactOrg() {
        return contactOrg;
    }

    /**
     * @param contactOrg the contactOrg to set
     */
    public void setContactOrg(String contactOrg) {
        this.contactOrg = contactOrg;
    }

    /**
     * @return the contactPhone
     */
    public String getContactPhone() {
        return contactPhone;
    }

    /**
     * @param contactPhone the contactPhone to set
     */
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    /**
     * @return the softwareAuthorDetails
     */
    public List<Developer> getSoftwareAuthorDetails() {
        return softwareAuthorDetails;
    }

    /**
     * @param softwareAuthorDetails the softwareAuthorDetails to set
     */
    public void setSoftwareAuthorDetails(List<Developer> softwareAuthorDetails) {
        this.softwareAuthorDetails = softwareAuthorDetails;
    }

    /**
     * @return the doi
     */
    public String getDoi() {
        return doi;
    }

    /**
     * @param doi the doi to set
     */
    public void setDoi(String doi) {
        this.doi = doi;
    }

    /**
     * @return the contactName
     */
    public String getContactName() {
        return contactName;
    }

    /**
     * @param contactName the contactName to set
     */
    public void setContactName(String contactName) {
        this.contactName = contactName;
    }
    
    /**
     * Obtain this Object as a JSON value.
     * @return a JsonNode representing this Object's data
     */
    public JsonNode toJson() {
        return mapper.valueToTree(this);
    }
    
    /**
     * Obtain the JSON of this Object as a String.
     * 
     * @return the String of JSON representing this Object data
     */
    public String toJsonString() {
        return toJson().toString();
    }

    /**
     * @return the codeId
     */
    public Long getCodeId() {
        return codeId;
    }

    /**
     * @param codeId the codeId to set
     */
    public void setCodeId(Long codeId) {
        this.codeId = codeId;
    }

    /**
     * @return the ostiId
     */
    public Long getOstiId() {
        return ostiId;
    }

    /**
     * @param ostiId the ostiId to set
     */
    public void setOstiId(Long ostiId) {
        this.ostiId = ostiId;
    }

    /**
     * @return the softId
     */
    public Long getSoftId() {
        return softId;
    }

    /**
     * @param softId the softId to set
     */
    public void setSoftId(Long softId) {
        this.softId = softId;
    }

    /**
     * @return the doiInfix
     */
    public String getDoiInfix() {
        return doiInfix;
    }

    /**
     * @param doiInfix the doiInfix to set
     */
    public void setDoiInfix(String doiInfix) {
        this.doiInfix = doiInfix;
    }
}
