/*
 */
package gov.osti.entity;

import gov.osti.entity.DOECodeMetadata.Status;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * Implement a primary composite key for the METADATA_SNAPSHOT Entity.
 *
 * @author ensornl
 */
@Embeddable
public class MetadataSnapshotKey implements Serializable {
    @Column (name = "code_id")
    private Long codeId;
    @Enumerated (EnumType.STRING)
    @Column (name = "snapshot_status")
    private Status snapshotStatus;

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

    @Override
    public boolean equals(Object o) {
        if (o instanceof MetadataSnapshotKey ) {
            return ((MetadataSnapshotKey)o).getCodeId().equals(getCodeId()) &&
                   ((MetadataSnapshotKey)o).getSnapshotStatus().equals(getSnapshotStatus());
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.codeId);
        hash = 83 * hash + Objects.hashCode(this.snapshotStatus);
        return hash;
    }
}
