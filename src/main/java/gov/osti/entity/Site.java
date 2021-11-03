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
public class Site implements Serializable {

    private String siteCode;
    private List<String> emailDomains;
    private List<String> pocEmails;
    private String lab;
    private boolean standardUsage = false;
    private boolean hqUsage = false;
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

        for (int i = 0; i < pocEmails.size(); i++)
            pocEmails.set(i, pocEmails.get(i).toLowerCase());

        this.pocEmails = pocEmails;
    }

    public String getLab() {
        return lab;
    }

    public void setLab(String lab) {
        this.lab = lab;
    }

    @Column(name = "standard_usage", nullable = false)
    public boolean isStandardUsage() {
        return standardUsage;
    }

    public void setStandardUsage(boolean usage) {
        this.standardUsage = usage;
    }

    @Column(name = "hq_usage", nullable = false)
    public boolean isHqUsage() {
        return hqUsage;
    }

    public void setHqUsage(boolean usage) {
        this.hqUsage = usage;
    }

    @Column(name = "SOFTWARE_GROUP_EMAIL")
    public String getSoftwareGroupEmail() {
        return softwareGroupEmail;
    }

    public void setSoftwareGroupEmail(String email) {
        this.softwareGroupEmail = email;
    }
}
