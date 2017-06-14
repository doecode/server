package gov.osti.security;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;

import gov.osti.entity.User;

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
		SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();		
		User currentUser = (User) principals.asList().get(0);
		info.setRoles(currentUser.getRoles());
		return info;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {		
		return new BearerAuthenticationInfo((BearerAuthenticationToken) token);
	}
	
	//ensure that there was in fact a user
	@Override
	protected void assertCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) throws AuthenticationException {
	    User currentUser = (User) token.getPrincipal();
	    if (currentUser == null)
	        throw new AuthenticationException("Could not find user");
	}

	
	
}
