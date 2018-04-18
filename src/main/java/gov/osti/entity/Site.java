package gov.osti.entity;

import java.io.Serializable;
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
@Table(name="sites")
@NamedQueries ({
    @NamedQuery (name = "Site.findByDomain", query = "SELECT s FROM Site s JOIN s.emailDomains d WHERE d = lower(:domain)")
})
public class Site implements Serializable {

    private String siteCode;
    private List<String> emailDomains;
    private String lab;

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

    public String getLab() {
        return lab;
    }

    public void setLab(String lab) {
        this.lab = lab;
    }
}
