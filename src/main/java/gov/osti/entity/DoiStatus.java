/*
 */
package gov.osti.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Implement a "DOI Status" table in order to track registered DOI values within DOECode.
 * 
 * Each registered DOI should belong to exactly one Metadata record.  In order to avoid
 * DOI collections or misappropriated values, check prior to registration.
 * 
 * @author ensornl
 */
@Entity
@Table (name="doi_status")
@NamedQueries({
    @NamedQuery(name = "DoiStatus.findByDoi", query = "SELECT s FROM DoiStatus s WHERE UPPER(s.doi)=UPPER(:doi)"),
    @NamedQuery(name = "DoiStatus.findByCodeId", query = "SELECT s FROM DoiStatus s WHERE s.codeId=:codeId")
})
public class DoiStatus implements Serializable {
    @Id
    private String doi;
    private Long codeId;
    @Basic(optional = false)
    @Column(name = "date_record_added")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateRecordAdded;
    @Basic(optional = false)
    @Column(name = "date_record_updated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateRecordUpdated;

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
     * @return the dateRecordAdded
     */
    public Date getDateRecordAdded() {
        return dateRecordAdded;
    }

    /**
     * @param dateRecordAdded the dateRecordAdded to set
     */
    public void setDateRecordAdded(Date dateRecordAdded) {
        this.dateRecordAdded = dateRecordAdded;
    }
    
    public void setDateRecordAdded () {
        setDateRecordAdded(new Date());
    }

    /**
     * @return the dateRecordUpdated
     */
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
    
    /**
     * Method called when a record is first created.  Sets dates added and
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
}
