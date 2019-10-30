
package gov.osti.entity;

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

/**
 * A storage cache Entity for Approved Metadata values.
 *
 * @author nensor@gmail.com
 */
@Entity
@Table (name = "metadata_snapshot",
        uniqueConstraints = {
            @UniqueConstraint (columnNames = {"code_id", "snapshot_status"})
        }
        )
@JsonIgnoreProperties (ignoreUnknown = true)
@NamedQueries ({
    @NamedQuery (name = "MetadataSnapshot.findByCodeIdAndStatus", query = "SELECT s FROM MetadataSnapshot s WHERE s.snapshotKey.codeId=:codeId AND s.snapshotKey.snapshotStatus=:status"),
    @NamedQuery (name = "MetadataSnapshot.findAllByStatus", query = "SELECT s FROM MetadataSnapshot s WHERE s.snapshotKey.snapshotStatus=:status ORDER BY s.snapshotKey.codeId"),
    @NamedQuery (name = "MetadataSnapshot.findByCodeIdLastNotStatus", query = "SELECT s FROM MetadataSnapshot s WHERE s.snapshotKey.codeId=:codeId AND s.snapshotKey.snapshotStatus<>:status ORDER BY s.dateRecordUpdated DESC"),
    @NamedQuery (name = "MetadataSnapshot.findByDoiAndStatus", query = "SELECT s FROM MetadataSnapshot s WHERE s.doi=:doi AND s.snapshotKey.snapshotStatus=:status ORDER BY s.snapshotKey.codeId"),
    @NamedQuery (name = "MetadataSnapshot.findByCodeIdAsSystemStatus", query = "SELECT ss FROM MetadataSnapshot s, MetadataSnapshot ss WHERE s.snapshotKey.codeId=:codeId AND s.snapshotKey.snapshotStatus=:status AND s.snapshotKey.codeId = ss.snapshotKey.codeId AND ss.snapshotKey.snapshotStatus <> :status AND ss.dateRecordAdded <= s.dateRecordUpdated ORDER BY ss.snapshotKey.snapshotStatus")
})
public class MetadataSnapshot implements Serializable {
    @EmbeddedId
    private MetadataSnapshotKey snapshotKey = new MetadataSnapshotKey();
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
    @Basic (optional = false)
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column (name = "date_record_updated", insertable = true, updatable = true)
    @Temporal (TemporalType.TIMESTAMP)
    private Date dateRecordUpdated;

    /**
     * Get the SnapshotKey of the Metadata Object.
     * @return a MetadataSnapshotKey object
     */
    public MetadataSnapshotKey getSnapshotKey() {
        return snapshotKey;
    }

    /**
     * Set the SnapshotKey of the Metadata Object.
     * @param key MetadataSnapshotKey object
     */
    public void setSnapshotKey(MetadataSnapshotKey key) {
        this.snapshotKey = key;
    }

    /**
     * Get the DOI of the Snapshot Object.
     * @return a String representing the DOI for this state
     */
    public String getDoi() {
        return doi;
    }

    /**
     * Set the DOI of the Snapshot Object.
     * @param doi DOI value
     */
    public void setDoi(String doi) {
        this.doi = doi;
    }

    /**
     * Get the Minted state of the DOI for the Snapshot Object.
     * @return a boolean representing the DOI Minted state
     */
    public boolean getDoiIsMinted() {
        return doiIsMinted;
    }

    /**
     * Set the Minted state of the DOI for the Snapshot Object.
     * @param isMinted boolean state of the Minted condition of the DOI for this Snapshot
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
