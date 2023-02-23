package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import gov.osti.services.Validation;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

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
    
    @Size (max = 50, message = "First name is limited to 50 characters.")
    @Column (name = "FIRST_NAME")
    public String getFirstName() {
            return firstName;
    }
    public void setFirstName(String firstName) {
            this.firstName = firstName;
    }
    @Size (max = 60, message = "Last name is limited to 60 characters.")
    @Column (name = "LAST_NAME")
    public String getLastName() {
            return lastName;
    }
    public void setLastName(String lastName) {
            this.lastName = lastName;
    }
    @Size (max = 50, message = "Middle name is limited to 50 characters.")
    @Column (name = "MIDDLE_NAME")
    public String getMiddleName() {
            return middleName;
    }
    public void setMiddleName(String middleName) {
            this.middleName = middleName;
    }

    @Size (max = 640, message = "Email is limited to 640 characters.")
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
            this.orcid = Validation.formatORCID(orcid, "dashed");
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

    protected List<String> CleanList(List<String> list) {
        if (list != null) {
            // remove empty data
            int cnt = list.size();
            for (int i = cnt - 1; i >= 0; i--) {
            String s = list.get(i);
            if (StringUtils.isBlank(s))
                    list.remove(i);
            }
        }

        return list;
    }
}
