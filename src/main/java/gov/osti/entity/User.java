package gov.osti.entity;

import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="users")
public class User {
	
	public User() {
		
	}
	
	public User(String email, String password, String apiKey, String siteId, String confirmationCode) {
		this.password = password;
		this.apiKey = apiKey;
		this.email = email;
		this.siteId = siteId;
		this.confirmationCode = confirmationCode;
	}
	
    @Id
	private String email = null;
	
	private String password = null;
	
	private String apiKey = null;
	
	private String confirmationCode = null;
	
	private String siteId = null;
	
	private boolean verified = false;
	
	@ElementCollection
	private Set<String> roles = null;
	
	@ElementCollection 
	private Set<String> pendingRoles = null;

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

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	public Set<String> getPendingRoles() {
		return pendingRoles;
	}

	public void setPendingRoles(Set<String> pendingRoles) {
		this.pendingRoles = pendingRoles;
	}
	
	
	
	
	

}
