package gov.osti.security;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.NewCookie;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.osti.entity.User;
import gov.osti.listeners.DoeServletContextListener;
import io.jsonwebtoken.Claims;

public class BearerTokenAuthenticatingFilter extends AuthenticatingFilter {
	private static Logger log = LoggerFactory.getLogger(BearerTokenAuthenticatingFilter.class);
	
	@Override
	protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
		HttpServletRequest req = (HttpServletRequest) request;
		//go through cookies and pull out accessToken
		Cookie[] cookies = req.getCookies();
		String cookieVal = null;
		if (cookies != null) {
			for (Cookie c : cookies) {
				if (StringUtils.equals(c.getName(), "accessToken"))
					cookieVal = c.getValue();
			}
		}
		String authorizationHeader = req.getHeader("Authorization");
		
		if (cookieVal == null &&  ( authorizationHeader == null || !authorizationHeader.startsWith("Basic "))) {
			throw new AuthenticationException("Authentication method not provided");
		}
		

		String apiKey = null;
		String xsrfToken = null;
		if (cookieVal != null) {
			Claims claims = DOECodeCrypt.parseJWT(cookieVal);
			xsrfToken = (String) claims.get("xsrfToken");
			String xsrfHeader = req.getHeader("X-XSRF-TOKEN");
			
			if (!StringUtils.equals(xsrfHeader,xsrfToken)) {
				throw new AuthenticationException("XSRF Tokens did not match");			
			}
			
			Date now = new Date();
			if (now.after(claims.getExpiration()))
				throw new AuthenticationException("Token is expired");
			
			apiKey = claims.getSubject();
			
		} else {
			apiKey = authorizationHeader.substring("Basic".length()).trim();
		}
		
	    EntityManager em = DoeServletContextListener.createEntityManager();
	    User currentUser = null;
	    try {        
                // only allow VERIFIED users
	        TypedQuery<User> getUserByApiKey = em.createQuery("SELECT u FROM User u WHERE u.apiKey = :apiKey AND u.verified = TRUE AND u.active = TRUE", User.class);
	        currentUser = getUserByApiKey
                        .setParameter("apiKey", apiKey)
                        .getSingleResult();
	    } catch ( Exception e ) {
                log.warn("Authentication Error: " + e.getMessage());
	        throw new AuthenticationException("Could not find user");
	    } finally {
	        em.close();  
	    }
	    
	    
	    if (cookieVal != null) {
			return new BearerAuthenticationToken(currentUser, apiKey, xsrfToken);
	    } else {
			return new BearerAuthenticationToken(currentUser, apiKey);
	    }
		
		
	
	}
	
	//on failure, we let it through to the endpoints. We assume they are correctly checking for authentication.
	@Override
	protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
		return true;
	}
	
	@Override	
	protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
		try {
			return executeLogin(request,response);
		} catch (Exception e) {
                    log.error("Access allowed error: " + e.getMessage());
			return false;
		}
	}
	
	
	//update cookie/jwt expiration and reissue if there is, this time with updated expiration info
	@Override
	protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response) throws Exception {
		HttpServletResponse res = (HttpServletResponse) response;
		BearerAuthenticationToken bat = (BearerAuthenticationToken) token;
		//only reissue cookie if it has an XSRF token e.g. means this was a cookie request
		if (StringUtils.isNotBlank(bat.getXsrfToken())) {
			String accessToken = DOECodeCrypt.generateLoginJWT((String) bat.getCredentials(), bat.getXsrfToken());
			NewCookie cookie = DOECodeCrypt.generateNewCookie(accessToken);
			res.setHeader("SET-COOKIE", cookie.toString());
		}
		return true;
	}

}
