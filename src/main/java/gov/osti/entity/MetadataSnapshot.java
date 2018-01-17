
package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import gov.osti.entity.DOECodeMetadata.Status;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
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
}
