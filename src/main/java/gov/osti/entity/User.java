package gov.osti.entity;

import java.util.HashSet;
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
	
	public User(String email, String password, String apiKey, String siteId) {
		this.password = password;
		this.apiKey = apiKey;
		this.email = email;
		this.siteId = siteId;
	}
	
    @Id
	private String email = null;
	
	private String password = null;
	
	private String apiKey = null;
	

	
	private String siteId = null;
	
	@ElementCollection
	private Set<String> roles = null;

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

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(HashSet<String> roles) {
		this.roles = roles;
	}
	
	
	

}
