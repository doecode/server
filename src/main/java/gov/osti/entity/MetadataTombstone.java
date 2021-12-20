
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
 * A storage cache Entity for Tombstone Metadata values.
 *
 * @author sowerst
 */
@Entity
@Table (name = "metadata_tombstone",
        uniqueConstraints = {
            @UniqueConstraint (columnNames = {"code_id", "tombstone_status"})
        }
        )
@JsonIgnoreProperties (ignoreUnknown = true)
@NamedQueries ({
    @NamedQuery (name = "MetadataTombstone.findLatestByCodeId", query = "SELECT s FROM MetadataTombstone s WHERE s.tombstoneKey.tombstoneStatus NOT IN (gov.osti.entity.MetadataTombstone.Status.Unhidden) AND s.tombstoneKey.codeId=:codeId ORDER BY s.dateRecordAdded DESC")
})
public class MetadataTombstone implements Serializable {

    /**
     * Record states/work flow:
     * Hidden - removed from system, potential for restoration
     * Unhidden - restored to system
     * Deleted - removed from system, no potential for restoration
     */
    public enum Status {
        Hidden,
        Unhidden,
        Deleted
    }


    @EmbeddedId
    private MetadataTombstoneKey tombstoneKey = new MetadataTombstoneKey();
    @Column (name = "doi")
    private String doi;
    @Column (name = "doi_is_minted", nullable = false)
    private boolean doiIsMinted = false;
    @Lob
    @Column (name = "json")
    private String json;
    @Lob
    @Column (name = "approved_json")
    private String approvedJson;
    // administrative dates
    @Basic (optional = false)
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column (name = "date_record_added", insertable = true, updatable = false)
    @Temporal (TemporalType.TIMESTAMP)
    private Date dateRecordAdded;
    @Column (name = "restricted_metadata")
    private boolean restrictedMetadata;

    /**
     * Get the tombstoneKey of the Metadata Object.
     * @return a MetadataTombstoneKey object
     */
    public MetadataTombstoneKey getTombstoneKey() {
        return tombstoneKey;
    }

    /**
     * Set the tombstoneKey of the Metadata Object.
     * @param key MetadataTombstoneKey object
     */
    public void setTombstoneKey(MetadataTombstoneKey key) {
        this.tombstoneKey = key;
    }

    /**
     * Get the DOI of the Tombstone Object.
     * @return a String representing the DOI for this state
     */
    public String getDoi() {
        return doi;
    }

    /**
     * Set the DOI of the Tombstone Object.
     * @param doi DOI value
     */
    public void setDoi(String doi) {
        this.doi = doi;
    }

    /**
     * Get the Minted state of the DOI for the Tombstone Object.
     * @return a boolean representing the DOI Minted state
     */
    public boolean getDoiIsMinted() {
        return doiIsMinted;
    }

    /**
     * Set the Minted state of the DOI for the Tombstone Object.
     * @param isMinted boolean state of the Minted condition of the DOI for this Tombstone
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
     * Get the approved JSON storage of the Metadata Object.
     * @return a JSON String representing the Metadata value state
     */
    public String getApprovedJson() {
        return approvedJson;
    }

    /**
     * Set the JSON of the Metadata Object.
     * @param json JSON containing the approved state of the Metadata values
     */
    public void setApprovedJson(String json) {
        this.approvedJson = json;
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

    /**
     * @return the restrictedMetadata value
     */
    public boolean getRestrictedMetadata() {
        return restrictedMetadata;
    }

    /**
     * @param restrictedMetadata the restrictedMetadata to set
     */
    public void setRestrictedMetadata(boolean restrictedMetadata) {
        this.restrictedMetadata = restrictedMetadata;
    }
}
