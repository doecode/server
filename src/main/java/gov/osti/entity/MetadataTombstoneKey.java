/*
 */
package gov.osti.entity;

import gov.osti.entity.MetadataTombstone.Status;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * Implement a primary composite key for the METADATA_TOMBSTONE Entity.
 *
 * @author sowerst
 */
@Embeddable
public class MetadataTombstoneKey implements Serializable {
    @Column (name = "code_id")
    private Long codeId;
    @Enumerated (EnumType.STRING)
    @Column (name = "tombstone_status")
    private Status tombstoneStatus;

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
     * @return the tombstoneStatus
     */
    public Status getTombstoneStatus() {
        return tombstoneStatus;
    }

    /**
     * @param tombstoneStatus the tombstoneStatus to set
     */
    public void setTombstoneStatus(Status tombstoneStatus) {
        this.tombstoneStatus = tombstoneStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MetadataTombstoneKey ) {
            return ((MetadataTombstoneKey)o).getCodeId().equals(getCodeId()) &&
                   ((MetadataTombstoneKey)o).getTombstoneStatus().equals(getTombstoneStatus());
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.codeId);
        hash = 83 * hash + Objects.hashCode(this.tombstoneStatus);
        return hash;
    }
}
