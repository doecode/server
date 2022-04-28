package gov.osti.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "sites")
@NamedQueries({
    @NamedQuery(name = "Site.findByDomain", query = "SELECT s FROM Site s JOIN s.emailDomains d WHERE d = lower(:domain)")
    ,
    @NamedQuery(name = "Site.findBySiteCode", query = "SELECT s FROM Site s WHERE s.siteCode = :site")
    ,
    @NamedQuery(name = "Site.findWithSoftwareGroupEmail", query = "SELECT s FROM Site s WHERE s.softwareGroupEmail IS NOT NULL ORDER BY s")
    ,
    @NamedQuery(name = "Site.findAll", query = "SELECT s FROM Site s ORDER BY s")
    ,
    @NamedQuery(name = "Site.findStandard", query = "SELECT s FROM Site s WHERE s.standardUsage = true ORDER BY s")
    ,
    @NamedQuery(name = "Site.findHQ", query = "SELECT s FROM Site s WHERE s.hqUsage = true ORDER BY s")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Site implements Serializable {

    private String siteCode;
    private List<String> emailDomains;
    private List<String> pocEmails;
    private String labName;
    private Boolean standardUsage;
    private Boolean hqUsage;
    private String softwareGroupEmail;

    public Site() {
    }

    @Id
    @Column(name = "SITE_CODE")
    public String getSiteCode() {
        return siteCode;
    }

    public void setSiteCode(String siteCode) {
        this.siteCode = siteCode;
    }

    @ElementCollection
    @CollectionTable(
            name = "EMAIL_DOMAINS",
            joinColumns = @JoinColumn(name = "SITE_CODE")
    )
    @Column(name = "EMAIL_DOMAIN")
    public List<String> getEmailDomains() {
        return emailDomains;
    }

    public void setEmailDomains(List<String> emailDomains) {
        for (int i = emailDomains.size() - 1; i >= 0; i--) {
            // do not allow empty strings
            if (StringUtils.isBlank(emailDomains.get(i)))
                emailDomains.remove(i);
            else
                emailDomains.set(i, emailDomains.get(i).toLowerCase());
        }

        this.emailDomains = emailDomains;
    }

    @ElementCollection
    @CollectionTable(
            name = "SOFTWARE_POC",
            joinColumns = @JoinColumn(name = "SITE_CODE")
    )
    @Column(name = "EMAIL")
    public List<String> getPocEmails() {
        return pocEmails;
    }

    public void setPocEmails(List<String> pocEmails) {
        if (pocEmails == null)
            pocEmails = new ArrayList<>();

        for (int i = pocEmails.size() - 1; i >= 0; i--) {
            // do not allow empty strings
            if (StringUtils.isBlank(pocEmails.get(i)))
                pocEmails.remove(i);
            else
                pocEmails.set(i, pocEmails.get(i).toLowerCase());
        }

        this.pocEmails = pocEmails;
    }

    @Column(name = "LAB_NAME")
    public String getLabName() {
        return labName;
    }

    public void setLabName(String lab) {
        this.labName = lab;
    }

    @Column(name = "standard_usage", nullable = false)
    public Boolean getStandardUsage() {
        return standardUsage;
    }

    public void setStandardUsage(Boolean usage) {
        this.standardUsage = usage;
    }

    public Boolean isStandardUsage() {
        return standardUsage;
    }

    @Column(name = "hq_usage", nullable = false)
    public Boolean getHqUsage() {
        return hqUsage;
    }

    public void setHqUsage(Boolean usage) {
        this.hqUsage = usage;
    }

    public Boolean isHqUsage() {
        return hqUsage;
    }

    @Column(name = "SOFTWARE_GROUP_EMAIL")
    public String getSoftwareGroupEmail() {
        return softwareGroupEmail;
    }

    public void setSoftwareGroupEmail(String email) {
        this.softwareGroupEmail = email;
    }
}
