package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for "Persons" or Agents:  currently parent class for Developers
 * and Contributors.
 * 
 * @author ensornl
 */
@MappedSuperclass
@JsonIgnoreProperties ( ignoreUnknown = true )
public class Agent implements Serializable {
    private static Logger log = LoggerFactory.getLogger(Agent.class);
    private Long agentId = 0L;
    private String email = "";
    private String orcid = "";
    private String firstName = "";
    private String lastName = "";
    private String middleName = "";

    public Agent() {

    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column (name = "AGENT_ID")
    @JsonIgnore
    public Long getAgentId() {
        return this.agentId;
    }
    
    public void setAgentId(Long id) {
        this.agentId = id;
    }
    
    @Column (name = "FIRST_NAME")
    public String getFirstName() {
            return firstName;
    }
    public void setFirstName(String firstName) {
            this.firstName = firstName;
    }
    @Column (name = "LAST_NAME")
    public String getLastName() {
            return lastName;
    }
    public void setLastName(String lastName) {
            this.lastName = lastName;
    }
    @Column (name = "MIDDLE_NAME")
    public String getMiddleName() {
            return middleName;
    }
    public void setMiddleName(String middleName) {
            this.middleName = middleName;
    }

    @Column (length = 640)
    public String getEmail() {
            return email;
    }
    public void setEmail(String email) {
            this.email = email;
    }

    

    public String getOrcid() {
            return orcid;
    }

    public void setOrcid(String orcid) {
            this.orcid = orcid;
    }
    	
    /**
     * Generate a "full name" for indexing purposes.  Should return
     * "Last, First Middle" will null-safe protection.
     * 
     * @return the Agent full name as a String
     */
    @Override
    public String toString() {
        return ((StringUtils.isEmpty(getLastName())) ? "" : getLastName() + ", ") +
               ((StringUtils.isEmpty(getFirstName()) ? " " : getFirstName() + " ")) +
               ((StringUtils.isEmpty(getMiddleName()) ? "" : getMiddleName()));
    }
}
