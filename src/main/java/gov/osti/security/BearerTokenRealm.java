package gov.osti.security;

import java.util.HashSet;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;

public class BearerTokenRealm extends AuthorizingRealm {

	private class BearerAuthenticationInfo implements AuthenticationInfo{
		private static final long serialVersionUID = 3123123245L;
		private final BearerAuthenticationToken token;
		
		BearerAuthenticationInfo(BearerAuthenticationToken token) {
			this.token = token;
		}

		@Override
		public Object getCredentials() {
			return token.getCredentials();
		}

		@Override
		public PrincipalCollection getPrincipals() {
			SimplePrincipalCollection principals = new SimplePrincipalCollection();
			principals.add(token.getPrincipal(), getName());
			return principals;
		}
	}
	public BearerTokenRealm() {
		setAuthenticationTokenClass(BearerAuthenticationToken.class);
	}
	
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		System.out.println("hello?");
		HashSet<String> roles = new HashSet<>();
		roles.add("Admin");
		SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
		
		info.setRoles(roles);
		return info;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		System.out.println("Doing a thing with the token");
		BearerAuthenticationToken bearerToken = (BearerAuthenticationToken) token;
		
		//use the ID and password
		String check = (String) bearerToken.getPrincipal();
		System.out.println("hmm");
		//System.out.println(check + "x");
		if ("123".equals(check)) {
			System.out.println("Valid user");
			return new BearerAuthenticationInfo(bearerToken);
			
		}
		
		return null;
	}
	
	//let it pass for the moment
	@Override
	protected void assertCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) throws AuthenticationException {
		System.out.println("Hello there");
	}

	
	
}
