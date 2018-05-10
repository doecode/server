
package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import gov.osti.entity.DOECodeMetadata.Status;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * A storage cache Entity for Approved Metadata values.
 * 
 * @author nensor@gmail.com
 */
@Entity
@IdClass (MetadataSnapshotKey.class)
@Table (name = "metadata_snapshot",
        uniqueConstraints = { 
            @UniqueConstraint (columnNames = {"code_id", "snapshot_status"})
        }
        )
@JsonIgnoreProperties (ignoreUnknown = true)
@NamedQueries ({
    @NamedQuery (name = "MetadataSnapshot.findByCodeIdAndStatus", query = "SELECT s FROM MetadataSnapshot s WHERE s.codeId=:codeId AND s.snapshotStatus=:status"),
    @NamedQuery (name = "MetadataSnapshot.findAllByStatus", query = "SELECT s FROM MetadataSnapshot s WHERE s.snapshotStatus=:status")
})
public class MetadataSnapshot implements Serializable {
    @Id
    @Column (name = "code_id")
    private Long codeId;
    @Id
    @Enumerated (EnumType.STRING)
    @Column (name = "snapshot_status")
    private Status snapshotStatus;
    @Lob
    @Column (name = "json")
    private String json;
    // administrative dates
    @Basic (optional = false)
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column (name = "date_record_added", insertable = true, updatable = false)
    @Temporal (TemporalType.TIMESTAMP)
    private Date dateRecordAdded;
    @Basic (optional = false)
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column (name = "date_record_updated", insertable = true, updatable = true)
    @Temporal (TemporalType.TIMESTAMP)
    private Date dateRecordUpdated;

    /**
     * Get the CODE ID identifier.
     * @return the codeId
     */
    public Long getCodeId() {
        return codeId;
    }

    /**
     * Set the unique CODE ID value.
     * @param codeId the codeId to set
     */
    public void setCodeId(Long codeId) {
        this.codeId = codeId;
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
     * @return the snapshotStatus
     */
    public Status getSnapshotStatus() {
        return snapshotStatus;
    }

    /**
     * @param snapshotStatus the snapshotStatus to set
     */
    public void setSnapshotStatus(Status snapshotStatus) {
        this.snapshotStatus = snapshotStatus;
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
        if (null==getDateRecordAdded())
            setDateRecordAdded();
        setDateRecordUpdated();
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
    
    /**
     * Set DATE UPDATED to now.
     */
    public void setDateRecordUpdated() {
        setDateRecordUpdated(new Date());
    }
}
