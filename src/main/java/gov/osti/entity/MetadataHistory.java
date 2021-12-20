
package gov.osti.entity;

import gov.osti.entity.DOECodeMetadata.Status;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

/**
 * A storage cache Entity for Approved Metadata values.
 *
 * @author sowerst
 */
@Entity
@Table (name = "metadata_history")
@JsonIgnoreProperties (ignoreUnknown = true)
public class MetadataHistory implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column (name = "history_id")
    private Long historyId;
    @Column (name = "code_id")
    private Long codeId;
    @Enumerated (EnumType.STRING)
    @Column (name = "history_status")
    private Status historyStatus;
    @Column (name = "doi")
    private String doi;
    @Column (name = "doi_is_minted", nullable = false)
    private boolean doiIsMinted = false;
    @Lob
    @Column (name = "json")
    private String json;
    // administrative dates
    @Basic (optional = false)
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column (name = "date_record_added", insertable = true, updatable = false)
    @Temporal (TemporalType.TIMESTAMP)
    private Date dateRecordAdded;

    // history ids
    public Long getHistoryId() {
        return this.historyId;
    }
    
    public void setHistoryId(Long id) {
        this.historyId = id;
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
     * @return the historyStatus
     */
    public Status getHistoryStatus() {
        return historyStatus;
    }

    /**
     * @param historyStatus the historyStatus to set
     */
    public void setHistoryStatus(Status historyStatus) {
        this.historyStatus = historyStatus;
    }

    /**
     * Get the DOI of the History Object.
     * @return a String representing the DOI for this state
     */
    public String getDoi() {
        return doi;
    }

    /**
     * Set the DOI of the History Object.
     * @param doi DOI value
     */
    public void setDoi(String doi) {
        this.doi = doi;
    }

    /**
     * Get the Minted state of the DOI for the History Object.
     * @return a boolean representing the DOI Minted state
     */
    public boolean getDoiIsMinted() {
        return doiIsMinted;
    }

    /**
     * Set the Minted state of the DOI for the History Object.
     * @param isMinted boolean state of the Minted condition of the DOI for this History
     */
    public void setDoiIsMinted(boolean isMinted) {
        this.doiIsMinted = isMinted;
    }

    /**
     * Get the JSON storage of the Metadata Object.
     * @return a JSON String representing the Metadata value state
     */
    public String getJson() {
        return json;
    }

    /**
     * Set the JSON of the Metadata Object.
     * @param json JSON containing the current state of the Metadata values
     */
    public void setJson(String json) {
        this.json = json;
    }

    /**
     * Method called when a record is first created.  Sets date added
     */
    @PrePersist
    void createdAt() {
        setDateRecordAdded();
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

    /**
     * Set the DATE ADDED to now.
     */
    public void setDateRecordAdded() {
        setDateRecordAdded(new Date());
    }
}
