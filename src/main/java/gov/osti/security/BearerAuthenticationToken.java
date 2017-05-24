package gov.osti.security;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.crypto.AesCipherService;

public class BearerAuthenticationToken implements AuthenticationToken {

	private String apiKey;
	public BearerAuthenticationToken(String apiKey) {
		this.apiKey = apiKey;
	}
	@Override
	public Object getCredentials() {
		// TODO Auto-generated method stub
		return apiKey;
	}

	@Override
	public Object getPrincipal() {
		// TODO Auto-generated method stub
		return apiKey;
	}

}
