package gov.osti.security;

import java.util.Date;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.osti.services.Login;
import io.jsonwebtoken.Claims;

public class BearerTokenAuthenticatingFilter extends AuthenticatingFilter {
	private static Logger log = LoggerFactory.getLogger(BearerTokenAuthenticatingFilter.class);
	@Override
	protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
		System.out.println("Creating token");
		HttpServletRequest req = (HttpServletRequest) request;
		String cookieVal = req.getHeader("Cookie");
		System.out.println(cookieVal);
		String authorizationHeader = req.getHeader("Authorization");
		
		if (cookieVal == null &&  ( authorizationHeader == null || !authorizationHeader.startsWith("Basic "))) {
			throw new AuthenticationException("Authentication method not provided");
		}
		

		if (cookieVal != null) {
			Claims claims = JWTCrypt.parseJWT(cookieVal);
			String xsrfToken = (String) claims.get("xsrfToken");
			String xsrfHeader = req.getHeader("X-XSRF-TOKEN");
			System.out.println(xsrfToken);
			
			if (!StringUtils.equals(xsrfHeader,xsrfToken)) {
				throw new AuthenticationException("XSRF Tokens did not match");			
			}
			
			Date now = new Date();
			if (now.after(claims.getExpiration()))
				throw new AuthenticationException("Token is expired");
			
			return new BearerAuthenticationToken(claims.getSubject());
			
		} else {
			return new BearerAuthenticationToken(authorizationHeader.substring("Basic".length()).trim());
		}
		
	
	}
	
	//do we redirect them to login? that would be my guess, but maybe we do that from the front end and stash away the store there...l
	@Override
	protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
		System.out.println("Denied access");
		return false;
	}
	
	@Override
	
	protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
		System.out.println("Say something");
		log.info("hello");
		try {
			return executeLogin(request,response);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Hit exception");
			System.out.println(e.getMessage());
			return false;
		}
	}
	
	
	//add updated bearer token header, this time with updated expiration info
	@Override
	protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response) throws Exception {
		HttpServletResponse res = (HttpServletResponse) response;
		BearerAuthenticationToken bat = (BearerAuthenticationToken) token;
		//String userId = (String) subject.getPrincipal();
		//System.out.println("User ID after success is... " + userId);
		System.out.println(bat.getXsrfToken());
		String accessToken = "{\"accessToken\": \"" + JWTCrypt.generateJWT("123", bat.getXsrfToken()) + "\" }";
		Cookie cookie = new Cookie("accessToken", accessToken);
		cookie.setSecure(true);
		//cookie.
		res.addCookie(cookie);
		System.out.println("Bingo");
		return true;
	}

}
