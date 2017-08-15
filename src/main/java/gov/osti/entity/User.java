package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="users")
public class User implements Serializable {
	
	public User() {
		
	}
	
	public User(String email, String password, String apiKey, String confirmationCode) {
		this.password = password;
		this.apiKey = apiKey;
		this.email = email;
		this.confirmationCode = confirmationCode;
	}
	
    @Id
	private String email = null;
	
	private String password = null;
	
	private String apiKey = null;
	
	private String confirmationCode = null;
	
	private String siteId = null;
	
        // whether or not the account has been VERIFIED/CONFIRMED via email
	private boolean verified = false;
        // if the account has been administratively DISABLED or not
        private boolean active = false;
        
        private String firstName;
        private String lastName;
        
        // for CONTRACTOR entries; required and validated
        private String contractNumber;
	
	@ElementCollection
	private Set<String> roles = null;
	
	@ElementCollection 
	private Set<String> pendingRoles = null;

        @JsonIgnore
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	

	public String getConfirmationCode() {
		return confirmationCode;
	}

	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSiteId() {
		return siteId;
	}

	public void setSiteId(String siteId) {
		this.siteId = siteId;
	}

	public boolean isVerified() {
		return verified;
	}

	public void setVerified(boolean verified) {
		this.verified = verified;
	}
        
        public boolean isActive() {
            return active;
        }
        
        public void setActive(boolean active) {
            this.active = active;
        }

        /**
         * Get the currently approved ROLES
         * @return a Set of ROLES for this User
         */
	public Set<String> getRoles() {
		return roles;
	}

        /**
         * Set the approved ROLES for this User
         * @param roles the Set of approved ROLES
         */
	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

        /**
         * Retrieve a Set of unapproved/pending ROLES.
         * 
         * @return a Set of pending ROLES
         */
	public Set<String> getPendingRoles() {
		return pendingRoles;
	}

        /**
         * Set the PENDING ROLES
         * @param pendingRoles 
         */
	public void setPendingRoles(Set<String> pendingRoles) {
		this.pendingRoles = pendingRoles;
	}

    /**
     * Get the FIRST NAME
     * @return the firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Set the FIRST NAME
     * @param firstName the firstName to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Get the LAST NAME
     * @return the lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Set the LAST NAME
     * @param lastName the lastName to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Get the CONTRACT NUMBER for this User.
     * 
     * @return the contractNumber a CONTRACT NUMBER if any
     */
    public String getContractNumber() {
        return contractNumber;
    }

    /**
     * Set the CONTRACT NUMBER for this User
     * @param contractNumber the contractNumber to set
     */
    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }
	
	

}
