package gov.osti.security;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.crypto.AesCipherService;

public class BearerAuthenticationToken implements AuthenticationToken {

	private String apiKey = null;
	private String xsrfToken = null;
	public BearerAuthenticationToken(String apiKey) {
		this.apiKey = apiKey;
	}
	
	public BearerAuthenticationToken(String apiKey, String xsrfToken) {
		this.apiKey = apiKey;
		this.xsrfToken = xsrfToken;
	}
	
	@Override
	public Object getCredentials() {
		return apiKey;
	}

	@Override
	public Object getPrincipal() {
		return apiKey;
	}
	
	public String getXsrfToken() {
		return xsrfToken;
	}

}
