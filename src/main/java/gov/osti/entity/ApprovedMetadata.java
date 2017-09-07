
package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * A storage cache Entity for Approved Metadata values.
 * 
 * @author nensor@gmail.com
 */
@Entity
@Table (name = "approved_metadata")
@JsonIgnoreProperties (ignoreUnknown = true)
@NamedQueries ({
    @NamedQuery (name = "ApprovedMetadata.findByCodeId", query = "SELECT a FROM ApprovedMetadata a WHERE a.codeId=:codeId"),
    @NamedQuery (name = "ApprovedMetadata.findAll", query = "SELECT a FROM ApprovedMetadata a")
})
public class ApprovedMetadata implements Serializable {
    @Id
    @Column (name = "code_id")
    private Long codeId;
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
}
