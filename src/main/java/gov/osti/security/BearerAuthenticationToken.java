package gov.osti.security;

import org.apache.shiro.authc.AuthenticationToken;

import gov.osti.entity.User;

public class BearerAuthenticationToken implements AuthenticationToken {

	private static final long serialVersionUID = -7879878900969539671L;
	private String apiKey = null;
	private User user = null;
	private String xsrfToken = null;
	public BearerAuthenticationToken(User user, String apiKey) {
		this.user = user;
		this.apiKey = apiKey;
	}
	
	public BearerAuthenticationToken(User user, String apiKey, String xsrfToken) {
		this.user = user;
		this.apiKey = apiKey;
		this.xsrfToken = xsrfToken;
	}
	
	@Override
	public Object getCredentials() {
		return apiKey;
	}

	@Override
	public Object getPrincipal() {
		return user;
	}
	
	public String getXsrfToken() {
		return xsrfToken;
	}

}
