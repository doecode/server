package gov.osti.entity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.io.Serializable;
import java.util.Objects;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.osti.entity.OfficialUseOnly.Protection;

import org.apache.commons.lang3.StringUtils;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Lob;


/**
 * POJO for interpreting CHANGE LOG information. 
 * .The VALUE will be an array of these in JSON.
 */
@Embeddable
@JsonIgnoreProperties ( ignoreUnknown = true )
public class ChangeLog implements Serializable {
    /**
     * Enumeration of valid Log Change list types.
     */
    public enum ListType{
        String,
        Other // different types, once comparators are created
    }

    // logger
    private static final Logger log = LoggerFactory.getLogger(Award.class);
    // data attributes for a ChangeLog
    private Date changeDate = new Date();
    private String changedBy = "Unknown";
    private String changesMade = "";

    public ChangeLog() {

    }

    // Jackson object mapper
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(Include.NON_NULL);


    @Column (name = "change_date")
    @Temporal (TemporalType.TIMESTAMP)
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getChangeDate() {
        return changeDate;
    }
 
    public void setChangeDate(Date date) {
        // always now
        this.changeDate = date;
    }

    @Column (name = "changed_by")
    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = StringUtils.trimToEmpty(changedBy);
    }

    @Lob
    @Column (name = "changes_made")
    public String getChangesMade() {
        return changesMade;
    }

    public void setChangesMade(String changes) {
        this.changesMade = StringUtils.trimToEmpty(changes);
    }

    
    public void LogChanges(DOECodeMetadata old, DOECodeMetadata current) {
        // REPOSITORY INFORMATION
        LogChanges("Project Type", (old.getProjectType() != null ? old.getProjectType().label() : null), (current.getProjectType() != null ? current.getProjectType().label() : null));
        // LogChanges("Site Ownership Code", old.getSiteOwnershipCode(), current.getSiteOwnershipCode());
        LogChanges("Open Source", (old.getOpenSource() != null ? old.getOpenSource().toString() : null), (current.getOpenSource() != null ? current.getOpenSource().toString() : null));
        LogChanges("Repository Link", old.getRepositoryLink(), current.getRepositoryLink());
        LogChanges("Landing Page", old.getLandingPage(), current.getLandingPage());
        // LogChanges("Software Type", old.getSoftwareType(), current.getSoftwareType());
        LogChanges("Landing Contact Email", old.getLandingContact(), current.getLandingContact());

        // PRODUCT DESCRIPTION
        LogChanges("Software Title", old.getSoftwareTitle(), current.getSoftwareTitle());
        LogChanges("Description", old.getDescription(), current.getDescription());
        LogChanges("Licenses", old.getLicenses(), current.getLicenses(), true, ListType.String);
        LogChanges("License URL", old.getProprietaryUrl(), current.getProprietaryUrl());
        LogChanges("License Contact Email", old.getLicenseContactEmail(), current.getLicenseContactEmail());
        LogChanges("Access Limitations", old.getAccessLimitations(), current.getAccessLimitations(), true, ListType.String);
        OfficialUseOnly ouoo = old.getOfficialUseOnly();
        OfficialUseOnly ouoc = current.getOfficialUseOnly();
        LogChanges("OUO Exemption Number", ouoo.getExemptionNumber(), ouoc.getExemptionNumber());
        LogChanges("OUO Protection", ouoo.getProtection(), ouoc.getProtection(), true); // can be null
        LogChanges("OUO Protection Other", ouoo.getProtectionOther(), ouoc.getProtectionOther());
        LogChanges("OUO Release Date", ouoo.getOuoReleaseDate(), ouoc.getOuoReleaseDate(), true);
        LogChanges("OUO Program Office", ouoo.getProgramOffice(), ouoc.getProgramOffice());
        LogChanges("OUO Protection Reason", ouoo.getProtectionReason(), ouoc.getProtectionReason());
        LogChanges("Programming Languages", old.getProgrammingLanguages(), current.getProgrammingLanguages(), true, ListType.String);
        LogChanges("Version Number", old.getVersionNumber(), current.getVersionNumber());
        LogChanges("Documentation URL", old.getDocumentationUrl(), current.getDocumentationUrl());

        // DEVELOPERS
        LogChanges("Developers", old.getDevelopers(), current.getDevelopers(), true, ListType.Other);

        // DOI
        LogChanges("DOI", old.getDoi(), current.getDoi());
        LogChanges("Release Date", old.getReleaseDate(), current.getReleaseDate(), true);

        // SUPPLEMENTAL
        LogChanges("Acronym", old.getAcronym(), current.getAcronym());
        LogChanges("Country of Origin", old.getCountryOfOrigin(), current.getCountryOfOrigin());
        LogChanges("Keywords", old.getKeywords(), current.getKeywords());
        LogChanges("Poject Keywords", old.getProjectKeywords(), current.getProjectKeywords(), true, ListType.String);
        LogChanges("Other Special Requirements", old.getOtherSpecialRequirements(), current.getOtherSpecialRequirements());
        LogChanges("SiteAccession Number", old.getSiteAccessionNumber(), current.getSiteAccessionNumber());
        LogChanges("Is Migration", Boolean.toString(old.getIsMigration()), Boolean.toString(current.getIsMigration()));
        LogChanges("File Name", old.getFileName(), current.getFileName());
        LogChanges("Is File Certified", Boolean.toString(old.getIsFileCertified()), Boolean.toString(current.getIsFileCertified()));
        LogChanges("Container Name", old.getContainerName(), current.getContainerName());

        // ORGS
        LogChanges("Sponsoring Organizations", old.getSponsoringOrganizations(), current.getSponsoringOrganizations(), true, ListType.Other);
        LogChanges("Research Organizations", old.getResearchOrganizations(), current.getResearchOrganizations(), true, ListType.Other);

        // CONTRIBS
        LogChanges("Contributors", old.getContributors(), current.getContributors(), true, ListType.Other);
        LogChanges("Contributing Organizations", old.getContributingOrganizations(), current.getContributingOrganizations(), true, ListType.Other);

        // IDENTIFIERS
        LogChanges("Related Identifiers", old.getRelatedIdentifiers(), current.getRelatedIdentifiers(), true, ListType.Other);

        // AWARDS
        LogChanges("Award DOIs", old.getAwardDois(), current.getAwardDois(), true, ListType.Other);

        // CONTACT INFO
        LogChanges("Contact Name", old.getRecipientName(), current.getRecipientName());
        LogChanges("Contact Email", old.getRecipientEmail(), current.getRecipientEmail());
        LogChanges("Contact Phone", old.getRecipientPhone(), current.getRecipientPhone());
        LogChanges("Contact Organization", old.getRecipientOrg(), current.getRecipientOrg());

        // COMMENTS
        LogChanges("Comments", old.getComment(), current.getComment());
    }

    public void LogChanges(String field, String old, String current) {
        LogChanges(field, old, current, false);
    }
    
    public void LogChanges(String field, String old, String current, boolean nullMeansRemove) {
        String msg = field;
        
        if (old == null && current == null)
            return;

        if (old != null && current == null) {
            if (!nullMeansRemove)
                return;

            msg = msg.concat(" removed.");
        }
        else if ((old == null && current != null) || (old != null && !old.equals(current)))
            //msg = msg.concat(" set to \"").concat(current).concat("\"");
            // "" means REMOVE, here
            if (StringUtils.isEmpty(current))
                msg = msg.concat(" removed.");
            else
                msg = msg.concat(" updated.");
        else
            return;

        LogChanges(msg);
    }
    
    // private void LogChanges(String field, List<String> old, List<String> current) {
    //     LogChanges(field, old, current, false);
    // }
    
    // private void LogChanges(String field, List<String> old, List<String> current, boolean nullMeansRemove) {
    //     String msg = field;
        
    //     if (old == null && current == null)
    //         return;

    //     if (old != null && current == null) {
    //         if (!nullMeansRemove)
    //             return;

    //         msg = msg.concat(" removed.");
    //     }
    //     else if ((old == null && current != null) || (old != null && !equalLists(old, current))) {
    //         // empty means REMOVE, here
    //         if (current.isEmpty())
    //             msg = msg.concat(" removed.");
    //         else
    //             msg = msg.concat(" updated.");
    //     }
    //     else
    //         return;

    //     LogChanges(msg);
    // }
    
    private void LogChanges(String field, Date old, Date current) {
        LogChanges(field, old, current, false);
    }
    
    private void LogChanges(String field, Date old, Date current, boolean nullMeansRemove) {
        String msg = field;
        
        if (old == null && current == null)
            return;

        if (old != null && current == null) {
            if (!nullMeansRemove)
                return;

            msg = msg.concat(" removed.");
        }
        else if ((old == null && current != null) || (old != null && !old.equals(current)))
            msg = msg.concat(" updated.");
        else
            return;

        LogChanges(msg);
    }
    
    private void LogChanges(String field, Protection old, Protection current, boolean nullMeansRemove) {
        String msg = field;
        
        if (old == null && current == null)
            return;

        if (old != null && current == null) {
            if (!nullMeansRemove)
                return;

            msg = msg.concat(" removed.");
        }
        else if ((old == null && current != null) || (old != null && !old.label().equals(current.label())))
            msg = msg.concat(" updated.");
        else
            return;

        LogChanges(msg);
    }
    
    private void LogChanges(String field, List old, List current, boolean nullMeansRemove, ListType lt) {
        String msg = field;
        
        if (old == null && current == null)
            return;

        if (old != null && current == null) {
            if (!nullMeansRemove || old.isEmpty())
                return;

            msg = msg.concat(" removed.");
        }
        else if (ListType.String.equals(lt) && ((old == null && current != null) || (old != null && !equalLists(old, current)))) {
            // empty means REMOVE, here
            if (current.isEmpty())
                msg = msg.concat(" removed.");
            else
                msg = msg.concat(" updated.");
        }
        else if (ListType.Other.equals(lt) && ((old == null && current != null) || (old != null && !equalListOther(old, current)))) {
            // empty means REMOVE, here
            if (current.isEmpty())
                msg = msg.concat(" removed.");
            else
                msg = msg.concat(" updated.");
        }
        else
            return;

        LogChanges(msg);
    }

    private boolean equalListOther(List old, List current) { 
        if (old == null && current == null){
            return true;
        }
    
        if ((old == null && current != null) 
            || old != null && current == null
            || old.size() != current.size()) {
            return false;
        }
    
        // Developer needs comparator added.

        return true;
    }

    private boolean equalLists(List<String> old, List<String> current) {  
        if (old == null && current == null){
            return true;
        }
    
        if ((old == null && current != null) 
            || old != null && current == null
            || old.size() != current.size()) {
            return false;
        }
    
        old = new ArrayList<String>(old); 
        current = new ArrayList<String>(current);   
    
        Collections.sort(old);
        Collections.sort(current);      
        return old.equals(current);
    }
    
    public void LogChanges(String msg) {
        if (!StringUtils.isBlank(changesMade))
            this.changesMade = changesMade.concat(" ");

        this.changesMade = changesMade.concat(msg);
    }
}
